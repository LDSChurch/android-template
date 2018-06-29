package org.jdc.template.inject

import android.app.Application
import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockWebServer
import org.jdc.template.Analytics
import org.jdc.template.TestFilesystem
import org.jdc.template.datasource.webservice.colors.ColorService
import org.jdc.template.util.CoroutineContextProvider
import org.mockito.AdditionalMatchers.or
import org.mockito.ArgumentMatchers.anyString
import org.mockito.ArgumentMatchers.isNull
import org.mockito.Mockito.`when`
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.mock
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
class CommonTestModule {
    @Provides
    @Singleton
    fun provideAnalytics(): Analytics {
        return object : Analytics {
            override fun send(params: Map<String, String>) {
                println(params.toString())
            }
        }
    }

    // ========== ANDROID ==========
    @Provides
    @Singleton
    internal fun provideApplication(): Application {
        val application = mock(Application::class.java)

        `when`(application.filesDir).thenReturn(TestFilesystem.INTERNAL_FILES_DIR)

        doAnswer { invocation ->
            val type = invocation.getArgument<String>(0)
            if (type != null) {
                return@doAnswer File(TestFilesystem.EXTERNAL_FILES_DIR, type)
            } else {
                return@doAnswer TestFilesystem.EXTERNAL_FILES_DIR
            }
        }.`when`(application).getExternalFilesDir(or(isNull(String::class.java), anyString()))

        return application
    }

//    @Provides
//    @Singleton
//    internal fun provideDatabaseManager(databaseConfig: DatabaseConfig): DatabaseManager {
//        val databaseManager = spy(DatabaseManager(databaseConfig))
//
//        // don't allow the database to be upgraded
//        doNothing().`when`(databaseManager).onUpgrade(MockitoKotlinHelper.any(), anyInt(), anyInt())
//
//        JdbcSqliteDatabaseWrapper.setEnableLogging(true)
//
//        return databaseManager
//    }

    @Provides
    @Singleton
    fun provideCoroutineContextProvider(): CoroutineContextProvider {
        return CoroutineContextProvider.TestCoroutineContextProvider
    }

    @Provides
    @Singleton
    fun provideOkHttp(): OkHttpClient {
        return OkHttpClient.Builder()
            .readTimeout(1, TimeUnit.SECONDS)
            .connectTimeout(1, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideGsonConverterFactory(): GsonConverterFactory {
        return GsonConverterFactory.create(Gson())
    }

    @Provides
    @Singleton
    fun provideColorService(mockWebServer: MockWebServer): ColorService {
        val retrofit = Retrofit.Builder()
            .baseUrl(mockWebServer.url(""))
            .client(provideOkHttp())
            .addConverterFactory(provideGsonConverterFactory())
            .build()

        return retrofit.create(ColorService::class.java)
    }

    @Provides
    @Singleton
    fun provideMockWebServer() = MockWebServer()
}
