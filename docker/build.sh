docker build ./DockerfileBaseImage --tag csye7200-base:1.0

docker build --pull --no-cache --tag csye7200-crawler:1.3 - < docker/DockerfileCrawler
