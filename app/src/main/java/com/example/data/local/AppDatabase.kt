package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.CallLogItem
import com.example.data.model.CallerIdItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [CallerIdItem::class, CallLogItem::class], version = 3, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun callerIdDao(): CallerIdDao
    abstract fun callLogDao(): CallLogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "dialerid_database"
                ).fallbackToDestructiveMigration()
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // Seed initial verified caller IDs
                        CoroutineScope(Dispatchers.IO).launch {
                            val dao = getDatabase(context).callerIdDao()
                            dao.insertAll(
                                listOf(
                                    CallerIdItem(
                                        id = "cid_1",
                                        phoneNumber = "+1 (800) 555-0199",
                                        label = "Company Headquarters",
                                        isPrimary = true,
                                        isVerified = false,
                                        countryCode = "US"
                                    ),
                                    CallerIdItem(
                                        id = "cid_2",
                                        phoneNumber = "+1 (415) 890-2134",
                                        label = "Direct Desk Line",
                                        isPrimary = false,
                                        isVerified = false,
                                        countryCode = "US"
                                    ),
                                    CallerIdItem(
                                        id = "cid_3",
                                        phoneNumber = "+44 20 7946 0912",
                                        label = "London Branch Office",
                                        isPrimary = false,
                                        isVerified = false,
                                        countryCode = "GB"
                                    )
                                )
                            )
                        }
                    }
                }).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
