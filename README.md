## Gtbot4s

Telegram bot, which receive Google Calendar events end send simple message with timings to Telegram in format:

```
🗓️ DayOfWeek(short) timeStart - endTime | 📍 Event Summary"
```

### Tech stack used:
- Scala3
- ZIO(http,json,schema,logging,config)
- sbt
- Telegram Bot API
- GCA (Google Cloud API - Calendar)
- Google Service Account IAM
- XML feed (CBR) for exchage rates info
Tools & Utils
- mise
- Zed editor (See my [blog post](https://lensgolda.github.io/posts/zed-scala3/) for Scala3 setup)
- VSCodium (Metals)
- Github Actions (for remote scheduled runs, see `.github/workflow/ci.yaml` config at project root)
- .env configuration (for local runs, with sbt-dotenv plugin)

### Usage

This is a normal sbt project. You can compile code with `sbt compile`, run it with `sbt run`, and `sbt console` will start a Scala 3 REPL.

For more information on the sbt-dotty plugin, see the
[scala3-example-project](https://github.com/scala/scala3-example-project/blob/main/README.md).
