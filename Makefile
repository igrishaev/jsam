download-json:
	wget https://github.com/seductiveapps/largeJSON/raw/master/100mb.json

.PHONY: repl
repl:
	lein repl

.PHONY: test
test:
	lein test
