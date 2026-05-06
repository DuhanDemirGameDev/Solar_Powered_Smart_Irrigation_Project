const http = require("http");
const fs = require("fs");
const path = require("path");

const root = __dirname;
const port = Number(process.env.PORT || 5500);

const contentTypes = {
    ".html": "text/html; charset=utf-8",
    ".js": "application/javascript; charset=utf-8",
    ".css": "text/css; charset=utf-8",
    ".json": "application/json; charset=utf-8"
};

const server = http.createServer((request, response) => {
    const requestUrl = new URL(request.url, `http://${request.headers.host}`);
    const safePath = path.normalize(decodeURIComponent(requestUrl.pathname)).replace(/^(\.\.[/\\])+/, "");
    const filePath = path.join(root, safePath === path.sep || safePath === "/" ? "index.html" : safePath);

    if (!filePath.startsWith(root)) {
        response.writeHead(403);
        response.end("Forbidden");
        return;
    }

    fs.readFile(filePath, (error, content) => {
        if (error) {
            response.writeHead(404);
            response.end("Not found");
            return;
        }

        response.writeHead(200, {
            "Content-Type": contentTypes[path.extname(filePath)] || "application/octet-stream"
        });
        response.end(content);
    });
});

server.listen(port, () => {
    console.log(`Frontend dashboard running at http://localhost:${port}`);
});
