all: lint test install

download-json:
	wget https://github.com/seductiveapps/largeJSON/raw/master/100mb.json

.PHONY: clear
clear:
	rm -rf target

.PHONY: repl
repl: clear
	lein repl

.PHONY: test
test:
	lein test

install:
	lein install

lint:
	clj-kondo --lint src

release: lint test
	lein release
