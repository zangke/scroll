package andy.book

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.*

/**
 * Created by andy on 2017-09-01.
 */
class ArticleProviderTest {
    @Test
    fun getArticleGroups() {
        val articleGroups = ArticleProvider.articleGroups()

        var list = ArrayList(articleGroups.keys) as MutableList<String>
        Collections.sort(list)

        assertEquals("羊皮卷", list.first())
    }

}