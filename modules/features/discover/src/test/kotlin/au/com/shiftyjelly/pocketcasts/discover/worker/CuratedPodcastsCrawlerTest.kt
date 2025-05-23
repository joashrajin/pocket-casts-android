package au.com.shiftyjelly.pocketcasts.discover.worker

import au.com.shiftyjelly.pocketcasts.models.entity.CuratedPodcast
import au.com.shiftyjelly.pocketcasts.repositories.lists.ListRepository
import au.com.shiftyjelly.pocketcasts.servers.di.ServersModule
import au.com.shiftyjelly.pocketcasts.servers.server.ListWebService
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.create

class CuratedPodcastsCrawlerTest {
    @get:Rule
    val server = MockWebServer()

    private lateinit var crawler: CuratedPodcastsCrawler

    @Before
    fun setup() {
        val moshi = ServersModule().provideMoshi()
        val service = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create<ListWebService>()
        val listRepository = ListRepository(listWebService = service, syncManager = null, platform = "android")
        crawler = CuratedPodcastsCrawler(listRepository, staticHostUrl = server.url("/static").toString())
    }

    @Test
    fun `crawl empty page`() = runTest {
        enqueueDiscoverPage("[]")

        val podcasts = crawler.crawl().getOrThrow()

        assertEqualsInAnyOrder(emptyList(), podcasts)
    }

    @Test
    fun `crawl single feed`() = runTest {
        enqueueDiscoverPage(
            """
            |[
            |  {
            |    "id": "bork",
            |    "curated": true,
            |    "sponsored": false,
            |    "type": "podcast_list",
            |    "title": "Bork!",
            |    "source": "${server.url("/")}",
            |    "summary_style": "",
            |    "expanded_style": "",
            |    "regions": []
            |  }
            |]
        """.trimMargin("|"),
        )
        enqueueFeed(
            """
            |{
            |  "list_id": "bork",
            |  "title": "Bork!",
            |  "podcasts": [
            |    {
            |      "uuid": "id-0",
            |      "title": "title-0"
            |    },
            |    {
            |      "uuid": "id-1",
            |      "title": "title-1",
            |      "description": "description-1"
            |    }
            |  ]
            |}
        """.trimMargin("|"),
        )

        val podcasts = crawler.crawl().getOrThrow()

        assertEqualsInAnyOrder(
            listOf(
                CuratedPodcast(
                    listId = "bork",
                    listTitle = "Bork!",
                    podcastId = "id-0",
                    podcastTitle = "title-0",
                    podcastDescription = null,
                ),
                CuratedPodcast(
                    listId = "bork",
                    listTitle = "Bork!",
                    podcastId = "id-1",
                    podcastTitle = "title-1",
                    podcastDescription = "description-1",
                ),
            ),
            podcasts,
        )
    }

    @Test
    fun `crawl multiple feeds`() = runTest {
        enqueueDiscoverPage(
            """
            |[
            |  {
            |    "id": "bork",
            |    "curated": true,
            |    "sponsored": false,
            |    "type": "podcast_list",
            |    "title": "Bork!",
            |    "source": "${server.url("/")}",
            |    "summary_style": "",
            |    "expanded_style": "",
            |    "regions": []
            |  },
            |  {
            |    "id": "smork",
            |    "curated": true,
            |    "sponsored": false,
            |    "type": "podcast_list",
            |    "title": "Smork!",
            |    "source": "${server.url("/")}",
            |    "summary_style": "",
            |    "expanded_style": "",
            |    "regions": []
            |  }
            |]
        """.trimMargin("|"),
        )
        enqueueFeed(
            """
            |{
            |  "list_id": "bork",
            |  "title": "Bork!",
            |  "podcasts": [
            |    {
            |      "uuid": "id-0",
            |      "title": "title-0"
            |    }
            |  ]
            |}
        """.trimMargin("|"),
        )
        enqueueFeed(
            """
            |{
            |  "list_id": "smork",
            |  "title": "Smork!",
            |  "podcasts": [
            |    {
            |      "uuid": "id-1",
            |      "title": "title-1"
            |    }
            |  ]
            |}
        """.trimMargin("|"),
        )

        val podcasts = crawler.crawl().getOrThrow()

        assertEqualsInAnyOrder(
            listOf(
                CuratedPodcast(
                    listId = "bork",
                    listTitle = "Bork!",
                    podcastId = "id-0",
                    podcastTitle = "title-0",
                    podcastDescription = null,
                ),
                CuratedPodcast(
                    listId = "smork",
                    listTitle = "Smork!",
                    podcastId = "id-1",
                    podcastTitle = "title-1",
                    podcastDescription = null,
                ),
            ),
            podcasts,
        )
    }

    @Test
    fun `crawl feeds even if fetching one feed fails`() = runTest {
        enqueueDiscoverPage(
            """
            |[
            |  {
            |    "id": "bork",
            |    "curated": true,
            |    "sponsored": false,
            |    "type": "podcast_list",
            |    "title": "Bork!",
            |    "source": "${server.url("/")}",
            |    "summary_style": "",
            |    "expanded_style": "",
            |    "regions": []
            |  },
            |  {
            |    "id": "smork",
            |    "curated": true,
            |    "sponsored": false,
            |    "type": "podcast_list",
            |    "title": "Smork!",
            |    "source": "${server.url("/")}",
            |    "summary_style": "",
            |    "expanded_style": "",
            |    "regions": []
            |  }
            |]
        """.trimMargin("|"),
        )
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.SHUTDOWN_INPUT_AT_END))
        enqueueFeed(
            """
            |{
            |  "list_id": "smork",
            |  "title": "Smork!",
            |  "podcasts": [
            |    {
            |      "uuid": "id-1",
            |      "title": "title-1"
            |    }
            |  ]
            |}
        """.trimMargin("|"),
        )

        val podcasts = crawler.crawl().getOrThrow()

        assertEqualsInAnyOrder(
            listOf(
                CuratedPodcast(
                    listId = "smork",
                    listTitle = "Smork!",
                    podcastId = "id-1",
                    podcastTitle = "title-1",
                    podcastDescription = null,
                ),
            ),
            podcasts,
        )
    }

    @Test
    fun `fail to crawl if discover page is not fetched`() = runTest {
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.SHUTDOWN_INPUT_AT_END))

        val result = crawler.crawl()

        assertTrue(result.isFailure)
    }

    @Test
    fun `fail to crawl if no feed is fetched`() = runTest {
        enqueueDiscoverPage(
            """
            |[
            |  {
            |    "id": "bork",
            |    "curated": true,
            |    "sponsored": false,
            |    "type": "podcast_list",
            |    "title": "Bork!",
            |    "source": "${server.url("/")}",
            |    "summary_style": "",
            |    "expanded_style": "",
            |    "regions": []
            |  },
            |  {
            |    "id": "smork",
            |    "curated": true,
            |    "sponsored": false,
            |    "type": "podcast_list",
            |    "title": "Smork!",
            |    "source": "${server.url("/")}",
            |    "summary_style": "",
            |    "expanded_style": "",
            |    "regions": []
            |  }
            |]
        """.trimMargin("|"),
        )
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.SHUTDOWN_INPUT_AT_END))
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.SHUTDOWN_INPUT_AT_END))

        val result = crawler.crawl()

        assertTrue(result.isFailure)
    }

    @Test
    fun `do not crawl sponsored feeds`() = runTest {
        enqueueDiscoverPage(
            """
            |[
            |  {
            |    "id": "bork",
            |    "curated": true,
            |    "sponsored": true,
            |    "type": "podcast_list",
            |    "title": "Bork!",
            |    "source": "${server.url("/")}",
            |    "summary_style": "",
            |    "expanded_style": "",
            |    "regions": []
            |  }
            |]
        """.trimMargin("|"),
        )

        val podcasts = crawler.crawl().getOrThrow()

        assertEqualsInAnyOrder(emptyList(), podcasts)
    }

    @Test
    fun `do not crawl not curated feeds`() = runTest {
        enqueueDiscoverPage(
            """
            |[
            |  {
            |    "id": "bork",
            |    "curated": false,
            |    "sponsored": false,
            |    "type": "podcast_list",
            |    "title": "Bork!",
            |    "source": "${server.url("/")}",
            |    "summary_style": "",
            |    "expanded_style": "",
            |    "regions": []
            |  }
            |]
        """.trimMargin("|"),
        )

        val podcasts = crawler.crawl().getOrThrow()

        assertEqualsInAnyOrder(emptyList(), podcasts)
    }

    @Test
    fun `do not crawl non podcast feeds`() = runTest {
        enqueueDiscoverPage(
            """
            |[
            |  {
            |    "id": "bork",
            |    "curated": true,
            |    "sponsored": false,
            |    "type": "episode_list",
            |    "title": "Bork!",
            |    "source": "${server.url("/")}",
            |    "summary_style": "",
            |    "expanded_style": "",
            |    "regions": []
            |  }
            |]
        """.trimMargin("|"),
        )

        val podcasts = crawler.crawl().getOrThrow()

        assertEqualsInAnyOrder(emptyList(), podcasts)
    }

    @Test
    fun `crawl trending list`() = runTest {
        enqueueDiscoverPage(
            """
            |[
            |  {
            |    "id": "trending",
            |    "curated": false,
            |    "sponsored": false,
            |    "type": "podcast_list",
            |    "title": "Trending",
            |    "source": "${server.url("/")}",
            |    "summary_style": "",
            |    "expanded_style": "",
            |    "regions": []
            |  }
            |]
        """.trimMargin("|"),
        )
        enqueueFeed(
            """
            |{
            |  "list_id": "trending",
            |  "title": "Trending!",
            |  "podcasts": [
            |    {
            |      "uuid": "id-0",
            |      "title": "title-0"
            |    }
            |  ]
            |}
        """.trimMargin("|"),
        )

        val podcasts = crawler.crawl().getOrThrow()

        assertEqualsInAnyOrder(
            listOf(
                CuratedPodcast(
                    listId = "trending",
                    listTitle = "Trending!",
                    podcastId = "id-0",
                    podcastTitle = "title-0",
                    podcastDescription = null,
                ),
            ),
            podcasts,
        )
    }

    @Test
    fun `crawl featured list`() = runTest {
        enqueueDiscoverPage(
            """
            |[
            |  {
            |    "id": "featured",
            |    "curated": false,
            |    "sponsored": false,
            |    "type": "podcast_list",
            |    "title": "Featured",
            |    "source": "${server.url("/featured.json")}",
            |    "summary_style": "",
            |    "expanded_style": "",
            |    "regions": []
            |  }
            |]
        """.trimMargin("|"),
        )
        enqueueFeed(
            """
            |{
            |  "list_id": "featured",
            |  "title": "Featured!",
            |  "podcasts": [
            |    {
            |      "uuid": "id-0",
            |      "title": "title-0"
            |    }
            |  ]
            |}
        """.trimMargin("|"),
        )

        val podcasts = crawler.crawl().getOrThrow()

        assertEqualsInAnyOrder(
            listOf(
                CuratedPodcast(
                    listId = "featured",
                    listTitle = "Featured!",
                    podcastId = "id-0",
                    podcastTitle = "title-0",
                    podcastDescription = null,
                ),
            ),
            podcasts,
        )

        // Skip initial request
        server.takeRequest()
        assertEquals("/static/engage/featured.json", server.takeRequest().path)
    }

    private fun enqueueDiscoverPage(layout: String) {
        val body = """
            |{
            |  "layout": $layout,
            |  "regions": {},
            |  "region_code_token": "",
            |  "region_name_token": "",
            |  "default_region_code": ""
            |}
        """.trimMargin("|")
        server.enqueue(MockResponse().setBody(body))
    }

    private fun enqueueFeed(body: String) {
        server.enqueue(MockResponse().setBody(body))
    }

    private fun <T> assertEqualsInAnyOrder(expected: List<T>, actual: List<T>) {
        assertEquals(expected.size, actual.size)
        assertTrue(actual.containsAll(expected))
    }
}
