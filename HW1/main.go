package main

import (
	"bufio"
	"fmt"
	"io"
	"log"
	"mime"
	"net/http"
	"net/url"
	"os"
	"path/filepath"
	"sort"
	"strings"
	"sync"
	"sync/atomic"
	"time"

	"golang.org/x/text/encoding/charmap"
	"golang.org/x/text/transform"
)

const (
	urlsPath    = "data/urls.txt"
	outputPath  = "output"
	workers     = 5
	reqTimeout  = 15 * time.Second
	domainPause = time.Second
)

type indexEntry struct {
	num int
	url string
}

var client = &http.Client{Timeout: reqTimeout}

func main() {
	src := urlsPath
	if len(os.Args) > 1 {
		src = os.Args[1]
	}

	urls, err := loadURLs(src)
	if err != nil {
		log.Fatalf("не удалось прочитать %s: %v", src, err)
	}
	if len(urls) == 0 {
		log.Fatal("список ссылок пуст")
	}
	log.Printf("ссылок: %d", len(urls))

	if err := os.MkdirAll(outputPath, 0755); err != nil {
		log.Fatalf("не удалось создать %s: %v", outputPath, err)
	}

	entries := crawl(urls)
	saveIndex(entries)
	log.Printf("успешно: %d/%d", len(entries), len(urls))
}

// loadURLs читает файл со ссылками, пропуская пустые строки и комментарии.
func loadURLs(path string) ([]string, error) {
	f, err := os.Open(path)
	if err != nil {
		return nil, err
	}
	defer f.Close()

	var urls []string
	sc := bufio.NewScanner(f)
	for sc.Scan() {
		line := strings.TrimSpace(sc.Text())
		if line != "" && !strings.HasPrefix(line, "#") {
			urls = append(urls, line)
		}
	}
	return urls, sc.Err()
}

// groupByHost раскладывает ссылки по доменам, сохраняя порядок.
func groupByHost(urls []string) map[string][]string {
	groups := make(map[string][]string)
	for _, u := range urls {
		host := hostFrom(u)
		groups[host] = append(groups[host], u)
	}
	return groups
}

// crawl группирует ссылки по домену — внутри группы запросы идут
// последовательно с паузой, чтобы не словить 429. Разные домены
// обрабатываются параллельно (до workers штук одновременно).
func crawl(urls []string) []indexEntry {
	groups := groupByHost(urls)

	var fileNum atomic.Int32  // порядковый номер файла
	var progress atomic.Int32 // сколько ссылок обработано (для лога)
	total := int32(len(urls))

	var mu sync.Mutex
	var entries []indexEntry

	sem := make(chan struct{}, workers)
	var wg sync.WaitGroup

	for _, group := range groups {
		wg.Add(1)
		go func(group []string) {
			defer wg.Done()
			sem <- struct{}{}
			defer func() { <-sem }()

			for i, link := range group {
				if i > 0 {
					time.Sleep(domainPause)
				}

				body, err := fetch(link)
				n := progress.Add(1)
				if err != nil {
					log.Printf("[%d/%d] ошибка %s: %v", n, total, link, err)
					continue
				}

				num, err := savePage(body, &fileNum)
				if err != nil {
					log.Printf("[%d/%d] ошибка записи %s: %v", n, total, link, err)
					continue
				}
				log.Printf("[%d/%d] ok %s -> %d.txt", n, total, link, num)

				mu.Lock()
				entries = append(entries, indexEntry{num, link})
				mu.Unlock()
			}
		}(group)
	}

	wg.Wait()
	return entries
}

// fetch скачивает страницу и возвращает тело в utf-8.
func fetch(pageURL string) ([]byte, error) {
	req, err := http.NewRequest(http.MethodGet, pageURL, nil)
	if err != nil {
		return nil, fmt.Errorf("создание запроса: %w", err)
	}

	// wikipedia блокирует запросы без user-agent (отдаёт 403)
	if strings.Contains(pageURL, "wikipedia.org") {
		req.Header.Set("User-Agent", "SportCrawler/1.0 (educational project; contact: student@example.com)")
	}

	resp, err := client.Do(req)
	if err != nil {
		return nil, fmt.Errorf("запрос: %w", err)
	}
	defer resp.Body.Close()

	if resp.StatusCode != http.StatusOK {
		return nil, fmt.Errorf("статус %d", resp.StatusCode)
	}

	return decodeBody(resp)
}

// savePage записывает тело страницы в файл с очередным порядковым номером.
func savePage(body []byte, counter *atomic.Int32) (int, error) {
	num := int(counter.Add(1))
	dst := filepath.Join(outputPath, fmt.Sprintf("%d.txt", num))
	return num, os.WriteFile(dst, body, 0644)
}

// decodeBody перекодирует тело ответа в utf-8, если сайт отдаёт
// windows-1251 или koi8-r (определяется по заголовку Content-Type).
func decodeBody(resp *http.Response) ([]byte, error) {
	cs := charsetFrom(resp.Header.Get("Content-Type"))

	var r io.Reader = resp.Body
	switch {
	case strings.Contains(cs, "1251"):
		r = transform.NewReader(resp.Body, charmap.Windows1251.NewDecoder())
	case strings.Contains(cs, "koi8"):
		r = transform.NewReader(resp.Body, charmap.KOI8R.NewDecoder())
	}
	return io.ReadAll(r)
}

// charsetFrom достаёт charset из Content-Type вида "text/html; charset=windows-1251".
func charsetFrom(ct string) string {
	_, params, err := mime.ParseMediaType(ct)
	if err != nil {
		return ""
	}
	return strings.ToLower(params["charset"])
}

func hostFrom(raw string) string {
	u, err := url.Parse(raw)
	if err != nil {
		return raw
	}
	return u.Hostname()
}

// saveIndex записывает индекс успешных загрузок, отсортированный по номеру файла.
func saveIndex(entries []indexEntry) {
	sort.Slice(entries, func(i, j int) bool {
		return entries[i].num < entries[j].num
	})

	path := filepath.Join(outputPath, "index.txt")
	f, err := os.Create(path)
	if err != nil {
		log.Fatalf("не удалось создать индекс: %v", err)
	}
	defer f.Close()

	w := bufio.NewWriter(f)
	for _, e := range entries {
		fmt.Fprintf(w, "%d\t%s\n", e.num, e.url)
	}
	w.Flush()
}
