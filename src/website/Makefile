.PHONY: dev build clean

# Local live-reloading server: http://localhost:1313
dev:
	hugo server -D --environment development

# Production build; output in ./public
build:
	hugo --minify --environment production

clean:
	rm -rf public resources/_gen
