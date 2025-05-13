build:
	mkdir -p out
	scala-cli --power package --native main.scala -f -o out/scaladex-mcp
