package app.glow.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/** Something the user did and wanted noticed, with the praise it got at the time. */
@Entity(tableName = "wins")
data class Win(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val at: Long,
    val text: String,
    /** 1 tiny, 2 solid, 3 huge; see [app.glow.domain.WinSize]. */
    val size: Int,
    /** Category id from [app.glow.domain.Category], or "" for none. */
    val category: String = "",
    /** The words said when it was saved, kept so the jar shows the same praise later. */
    val praise: String = "",
    /** Voice id that said [praise]. */
    val voice: String = "",
)

/** A compliment worth keeping: from a person, or a praise the user saved. */
@Entity(tableName = "kind_words")
data class KindWord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val at: Long,
    val text: String,
    /** Who said it; for saved praises, the voice id. */
    val from: String = "",
    /** File name of a screenshot in filesDir/kind, or null. */
    val image: String? = null,
    /** [SOURCE_PERSON] or [SOURCE_GLOW]. */
    val source: Int = SOURCE_PERSON,
) {
    companion object {
        const val SOURCE_PERSON = 0
        const val SOURCE_GLOW = 1
    }
}

/** Three things the user is proud of on one day ([day] is the epoch day). */
@Entity(tableName = "evenings")
data class Evening(
    @PrimaryKey val day: Long,
    val one: String,
    val two: String,
    val three: String,
    val at: Long,
) {
    val items: List<String> get() = listOf(one, two, three).filter { it.isNotBlank() }
}

class Snapshot(
    val wins: List<Win>,
    val kindWords: List<KindWord>,
    val evenings: List<Evening>,
)

@Dao
abstract class GlowDao {
    // Wins
    @Query("SELECT * FROM wins ORDER BY at")
    abstract fun observeWins(): Flow<List<Win>>

    @Query("SELECT * FROM wins ORDER BY at")
    abstract suspend fun wins(): List<Win>

    @Query("SELECT * FROM wins WHERE id = :id")
    abstract suspend fun win(id: Long): Win?

    @Insert
    abstract suspend fun insertWin(w: Win): Long

    @Update
    abstract suspend fun updateWin(w: Win)

    @Delete
    abstract suspend fun deleteWin(w: Win)

    // Kind words
    @Query("SELECT * FROM kind_words ORDER BY at DESC")
    abstract fun observeKindWords(): Flow<List<KindWord>>

    @Query("SELECT * FROM kind_words ORDER BY at DESC")
    abstract suspend fun kindWords(): List<KindWord>

    @Insert
    abstract suspend fun insertKindWord(k: KindWord): Long

    @Update
    abstract suspend fun updateKindWord(k: KindWord)

    @Delete
    abstract suspend fun deleteKindWord(k: KindWord)

    // Evenings
    @Query("SELECT * FROM evenings ORDER BY day DESC")
    abstract fun observeEvenings(): Flow<List<Evening>>

    @Query("SELECT * FROM evenings ORDER BY day DESC")
    abstract suspend fun evenings(): List<Evening>

    @Query("SELECT * FROM evenings WHERE day = :day")
    abstract suspend fun evening(day: Long): Evening?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun setEvening(e: Evening)

    @Delete
    abstract suspend fun deleteEvening(e: Evening)

    // Backup
    @Transaction
    open suspend fun snapshot() = Snapshot(wins(), kindWords(), evenings())

    @Query("DELETE FROM wins") protected abstract suspend fun clearWins()
    @Query("DELETE FROM kind_words") protected abstract suspend fun clearKindWords()
    @Query("DELETE FROM evenings") protected abstract suspend fun clearEvenings()

    @Insert protected abstract suspend fun insertWins(l: List<Win>)
    @Insert protected abstract suspend fun insertKindWords(l: List<KindWord>)
    @Insert protected abstract suspend fun insertEvenings(l: List<Evening>)

    @Transaction
    open suspend fun replaceAll(s: Snapshot) {
        clearWins(); clearKindWords(); clearEvenings()
        insertWins(s.wins); insertKindWords(s.kindWords); insertEvenings(s.evenings)
    }
}

@Database(
    entities = [Win::class, KindWord::class, Evening::class],
    version = 1,
    exportSchema = true,
)
abstract class GlowDatabase : RoomDatabase() {
    abstract fun dao(): GlowDao

    companion object {
        fun create(context: Context): GlowDatabase =
            Room.databaseBuilder(context.applicationContext, GlowDatabase::class.java, "glow.db").build()
    }
}
