package edu.neu.coe.csye7200

import java.net.{HttpURLConnection, URL}
import java.util.Base64
import ujson._

object SpotifyPlaylist {

  // ── PUT YOUR CREDENTIALS HERE ──────────────────────────────────────────
  val CLIENT_ID     = "8250e13cf51745e5a873fc8e8a1aa80d"
  val CLIENT_SECRET = "f94eb7b20ad3402f88a76293570054b9"
  // ───────────────────────────────────────────────────────────────────────

  val PLAYLIST_ID   = "5Rrf7mqN8uus2AaQQQNdc1"
  val MANUAL_TOKEN  = "BQCIu2VyIWhzSDmWEn3PK9WN7A6Rqe5iT80sQIuYU1Rhl-vphBikDKsr6Btflhkvmxegrf14CmUenkPA64epBl30STEySGhXqiMbjohIM-gbthWcL7P85brK1xA76fKe0eV0vcaU8GMJ61gSdv5bOdldxAFRja3qbSn_c1MRNO1TMjHl-OtBMwYCmYTY6OthVaDooieTAVNw-qNcgPJgNuHfAQvlTqlpLscKldqGSl6tzmPqPbzOGkH1CecCM6AL0SaYs8IJL6s4qEiQHSBG7dE_C7m92YnbcBr-pl4RYuhDX_jFc-IvTAiO_YKvF1WWxsJdGwQyDXzzsk4HThvG5dhYzInxAkTZLzifxmgMnF8CajnQq1k1Kv6n_kY9gKAdo1-JwGryEjZggNn7CB4huH2sj2i4"

  // Step 1: Get access token using Client Credentials flow
  def getAccessToken: String = {
    val url         = "https://accounts.spotify.com/api/token"
    val connection  = new URL(url).openConnection().asInstanceOf[HttpURLConnection]
    val credentials = Base64.getEncoder.encodeToString(s"$CLIENT_ID:$CLIENT_SECRET".getBytes)

    connection.setRequestMethod("POST")
    connection.setRequestProperty("Authorization", s"Basic $credentials")
    connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
    connection.setDoOutput(true)
    connection.getOutputStream.write("grant_type=client_credentials".getBytes)

    val stream   = connection.getInputStream
    val response = new String(stream.readAllBytes(), "UTF-8")
    stream.close()
    ujson.read(response)("access_token").str
  }

  // Helper: GET request with Bearer token
  def getJson(url: String, token: String): String = {
    val connection = new URL(url).openConnection().asInstanceOf[HttpURLConnection]
    connection.setRequestMethod("GET")
    connection.setRequestProperty("Authorization", s"Bearer $token")
    connection.setRequestProperty("Content-Type", "application/json")
    connection.connect()
    val stream = connection.getInputStream
    val bytes  = stream.readAllBytes()
    stream.close()
    new String(bytes, "UTF-8")
  }

  def main(args: Array[String]): Unit = {

    // ── Fetch token automatically ───────────────────────────────────────
    val token = if (MANUAL_TOKEN.nonEmpty && MANUAL_TOKEN != "PASTE_YOUR_CONSOLE_TOKEN_HERE")
      MANUAL_TOKEN
    else
      getAccessToken
    println("Token fetched successfully.\n")

    // ── Fetch playlist ──────────────────────────────────────────────────
    val playlistUrl  = s"https://api.spotify.com/v1/playlists/$PLAYLIST_ID"
    val playlistJson = getJson(playlistUrl, token)
    val playlistData = ujson.read(playlistJson)

    // ── Extract tracks ──────────────────────────────────────────────────
    val tracks = playlistData("tracks")("items").arr.toSeq.map { item =>
      val track      = item("track")
      val name       = track("name").str
      val durationMs = track("duration_ms").num.toLong
      val artists    = track("artists").arr.toSeq.map { a =>
        (a("name").str, a("id").str)
      }
      (name, durationMs, artists)
    }

    // ── Part 1: Top 10 longest songs ────────────────────────────────────
    val top10 = tracks.sortBy(-_._2).take(10)

    println("Part 1)")
    top10.foreach { case (name, duration, _) =>
      println(s"$name , $duration")
    }

    println()

    // ── Part 2: All unique artists from top 10, sorted by followers ─────
    val allArtists: Seq[(String, String)] = top10.flatMap(_._3).distinct

    val artistDetails: Seq[(String, Long)] = allArtists.map { case (_, artistId) =>
      val artistUrl  = s"https://api.spotify.com/v1/artists/$artistId"
      val artistJson = getJson(artistUrl, token)
      val artistData = ujson.read(artistJson)
      val name       = artistData("name").str
      val followers  = artistData("followers")("total").num.toLong
      (name, followers)
    }.sortBy(-_._2)

    println("Part 2)")
    artistDetails.foreach { case (name, followers) =>
      println(s"$name : $followers")
    }
  }
}