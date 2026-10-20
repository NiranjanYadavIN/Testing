package com.auravoice.tts.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.auravoice.tts.data.model.TtsEngineType

class Converters {
    @TypeConverter
    fun fromEngineType(engine: TtsEngineType): String = engine.name

    @TypeConverter
    fun toEngineType(value: String): TtsEngineType {
        if (value.equals("PIPER", ignoreCase = true) || value.equals("KOKORO", ignoreCase = true) || value.equals("ONNX", ignoreCase = true)) {
            return TtsEngineType.AURA_NEURAL
        }
        return runCatching { TtsEngineType.valueOf(value) }.getOrDefault(TtsEngineType.AURA_NEURAL)
    }
}

@Database(
    entities = [VoiceModelEntity::class, GenerationHistoryEntity::class],
    version = 4,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun voiceModelDao(): VoiceModelDao
    abstract fun generationHistoryDao(): GenerationHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "auravoice_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
