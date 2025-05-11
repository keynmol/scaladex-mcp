build:
	mkdir -p out
	scala-cli --power package main.scala -f -o out/scaladex-mcp --assembly
