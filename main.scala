//> using dep com.indoorvivants::mcp-quick::0.1.2
//> using dep com.softwaremill.sttp.client4::core::4.0.3
//> using scala 3.7.0

import mcp.*

/** This is a simple MCP server that exposes two tools that interact with
  * Scaladex
  */

@main def hello =
  MCPBuilder
    .create()
    .handle(initialize): req =>
      InitializeResult(
        capabilities =
          ServerCapabilities(tools = Some(ServerCapabilities.Tools())),
        serverInfo = Implementation("scaladex-mcp", "0.0.1"),
        protocolVersion = req.protocolVersion
      )
    .handle(tools.list): _ =>
      ListToolsResult(
        Seq(
          Tool(
            name = "scaladex_search",
            description = Some(
              """
              Full-text search over Scala libraries, with optional platform (JVM, JS, or Native) and scala version (2.12, 2.13, 3) parameters.
              Returns a JSON list of projects, with github organization and repository name, that can be then used as inputs to scaladex_project tool.
              """
            ),
            inputSchema = Tool.InputSchema(
              Some(
                ujson.Obj(
                  "query" -> ujson.Obj("type" -> ujson.Str("string")),
                  "platform" -> ujson.Obj("type" -> ujson.Str("string")),
                  "scalaVersion" -> ujson.Obj("type" -> ujson.Str("string"))
                )
              ),
              required = Some(Seq("query"))
            )
          ),
          Tool(
            name = "scaladex_project",
            description = Some(
              """
              Retrieves information about a Scala library, identified by organisation and repository name. 
              Returns a list of available artifacts, available versions, and maven coordinates (groupId, artifactId)
              """
            ),
            inputSchema = Tool.InputSchema(
              Some(
                ujson.Obj(
                  "organization" -> ujson.Obj("type" -> ujson.Str("string")),
                  "repository" -> ujson.Obj("type" -> ujson.Str("string"))
                )
              ),
              required = Some(Seq("organization", "repository"))
            )
          )
        )
      )
    .handle(tools.call): req =>
      req.name match
        case "scaladex_search" =>
          val args = req.arguments.get
          val query = args.obj("query").str.trim
          val platform = args.obj.get("platform").flatMap(_.strOpt)
          val scalaVersion = args.obj.get("scalaVersion").flatMap(_.strOpt)
          CallToolResult(content =
            Seq(
              TextContent(
                text = scaladex.search(query, platform, scalaVersion),
                `type` = "text"
              )
            )
          )
        case "scaladex_project" =>
          val args = req.arguments.get
          val organization = args.obj("organization").str.trim
          val repository = args.obj("repository").str.trim
          CallToolResult(content =
            Seq(
              TextContent(
                text = scaladex.project(organization, repository),
                `type` = "text"
              )
            )
          )
    .run(SyncTransport.default.verbose)

object scaladex:

  import sttp.client4.*

  val backend = DefaultSyncBackend()

  def search(
      query: String,
      platform: Option[String] = None,
      scalaVersion: Option[String] = None
  ) =
    basicRequest
      .get(
        uri"https://index.scala-lang.org/api/search?q=$query&target=${platform.getOrElse("JVM")}&scalaVersion=${scalaVersion.getOrElse("3")}"
      )
      .send(backend)
      .body
      .fold(sys.error(_), identity)

  def project(organization: String, repository: String) =
    basicRequest
      .get(
        uri"https://index.scala-lang.org/api/project?organization=$organization&repository=$repository"
      )
      .send(backend)
      .body
      .fold(sys.error(_), identity)

end scaladex
