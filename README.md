
# ManaDoksli

Upload images, extract text via OCR, and perform blazing-fast full-text search across your entire image library.


## Features

- Extract OCR and store images
- Full text search


## Quick set up on Docker

Clone the project

```bash
  git clone https://github.com/hakimamarullah/manadoksli-sv
```

Go to the project directory

```bash
  cd manadoksli-svc
```

Run docker-compose (Make sure you already have [Elasticsearch](https://www.elastic.co/docs/deploy-manage/deploy/self-managed/install-elasticsearch-with-docker) and [RustFS](https://docs.rustfs.com/installation/docker/) running)

```bash
  docker-compose up -d
```

Check actuator

```bash
  http://localhost:9898/actuator/health
```


## Acknowledgements

 - [OCR Space API](https://ocr.space/ocrapi)

