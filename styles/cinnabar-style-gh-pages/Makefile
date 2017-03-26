all: dist

dist:
	tangram-bundle cinnabar-style.yaml
	mv cinnabar-style.zip dist/cinnabar-style.zip

clean:
	rm -rf dist
	mkdir dist

tag:
	git tag  -m 'See CHANGELOG for details.' -a v`cat VERSION`
