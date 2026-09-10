"""Read-only HTML availability probe; does not save or print article text.

This is NOT a production content extractor or a claim of offline readability.
Only preselected public origins are allowed; redirects cannot escape them.
"""

import json
from html.parser import HTMLParser
from urllib.error import HTTPError, URLError
from urllib.parse import urlparse
from urllib.request import HTTPRedirectHandler, Request, build_opener


ALLOWED = {"wanandroid.com", "www.wanandroid.com", "juejin.cn"}
LIMIT = 1024 * 1024
SAMPLES = [
    "https://wanandroid.com/blog/show/3932",
    "https://www.wanandroid.com/wenda/show/26578",
    "https://juejin.cn/post/7682769917025189903",
]


def validate(url):
    parsed = urlparse(url)
    if parsed.scheme != "https" or parsed.hostname not in ALLOWED:
        raise ValueError("Unexpected origin")
    if parsed.username or parsed.password or parsed.port not in (None, 443):
        raise ValueError("Unexpected URL credentials/port")


class SafeRedirect(HTTPRedirectHandler):
    def redirect_request(self, req, fp, code, msg, headers, newurl):
        validate(newurl)
        return super().redirect_request(req, fp, code, msg, headers, newurl)


class TextStats(HTMLParser):
    def __init__(self):
        super().__init__()
        self.hidden = 0
        self.text_chars = 0
        self.paragraphs = 0
        self.article_elements = 0

    def handle_starttag(self, tag, attrs):
        if tag in ("script", "style"):
            self.hidden += 1
        if tag == "p":
            self.paragraphs += 1
        if tag == "article":
            self.article_elements += 1

    def handle_endtag(self, tag):
        if tag in ("script", "style"):
            self.hidden = max(0, self.hidden - 1)

    def handle_data(self, data):
        if not self.hidden:
            self.text_chars += len(data.strip())


def probe(url):
    validate(url)
    try:
        with build_opener(SafeRedirect()).open(
            Request(url, headers={"Accept": "text/html"}), timeout=20
        ) as response:
            payload = response.read(LIMIT + 1)
            if len(payload) > LIMIT:
                return {"url": url, "result": "too_large"}
            parser = TextStats()
            parser.feed(payload.decode("utf-8", errors="replace"))
            return {
                "url": url,
                "status": response.status,
                "bytes": len(payload),
                "text_chars_including_navigation": parser.text_chars,
                "paragraphs": parser.paragraphs,
                "article_elements": parser.article_elements,
            }
    except HTTPError as error:
        return {"url": url, "result": "http_error", "status": error.code}
    except (URLError, TimeoutError, ValueError) as error:
        return {"url": url, "result": type(error).__name__}


if __name__ == "__main__":
    for sample in SAMPLES:
        print(json.dumps(probe(sample), ensure_ascii=False))
