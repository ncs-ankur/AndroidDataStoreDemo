package com.ncs.poc.androiddatastoredemo

import android.content.ContentValues
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.room.Room
import com.google.android.material.divider.MaterialDivider
import com.ncs.poc.androiddatastoredemo.provider.MyDataProvider
import com.ncs.poc.androiddatastoredemo.room.AppDatabase
import com.ncs.poc.androiddatastoredemo.room.MyData
import com.ncs.poc.androiddatastoredemo.room.MyDataDao
import com.ncs.poc.androiddatastoredemo.sqlite.DBHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStream


class MainActivity : AppCompatActivity() {

    lateinit var dbHelper: DBHelper
    lateinit var sharedPreferences: SharedPreferences
    lateinit var myDataDao: MyDataDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        dbHelper = DBHelper(this)
        sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE)

        val db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java, ROOM_DB_NAME
        ).build()
        myDataDao = db.myDataDao()

        findViewById<View>(R.id.btnSave).setOnClickListener {
            saveData(findViewById<EditText>(R.id.edtData).text.toString())
            readData()
        }
    }

    override fun onStart() {
        super.onStart()
        readData()
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////

    private fun renderReadData(
        title: String,
        command: String?,
        data: String,
        path: String?,
    ) {
        val layoutParams = LinearLayout.LayoutParams(
            MATCH_PARENT,
            WRAP_CONTENT,
        )
        val dpInPx = (4 * resources.displayMetrics.density + 0.5f).toInt()
        layoutParams.setMargins(dpInPx, dpInPx, dpInPx, dpInPx)

        val group = LinearLayout(this)
        group.orientation = LinearLayout.VERTICAL

        title.let {
            group.addView(
                textWithStyle(
                    text = it,
                    styleRes = androidx.appcompat.R.style.TextAppearance_AppCompat_SearchResult_Title
                ), layoutParams
            )
        }

        command?.let {
            group.addView(
                textWithStyle(
                    text = it,
                    styleRes = androidx.appcompat.R.style.Base_TextAppearance_AppCompat_Caption
                ), layoutParams
            )
        }

        data.let {
            val textView = textWithStyle(
                text = it,
                styleRes = androidx.appcompat.R.style.Base_TextAppearance_AppCompat_SearchResult_Subtitle
            )
            if (data.isBlank()) {
                textView.text = "N/A"
                textView.setTextColor(getColor(com.google.android.material.R.color.design_error))
            } else {
                textView.setTextColor(getColor(com.google.android.material.R.color.abc_search_url_text_normal))
            }
            group.addView(
                textView, layoutParams
            )
        }

        path?.let {
            group.addView(
                textWithStyle(
                    text = it,
                    styleRes = androidx.appcompat.R.style.Base_TextAppearance_AppCompat_Tooltip
                ), layoutParams
            )
        }

        val divider = MaterialDivider(this)
        group.addView(divider, layoutParams)

        findViewById<LinearLayout>(R.id.listAll).addView(group, layoutParams)
    }

    private fun textWithStyle(text: String, styleRes: Int): TextView {
        /*
        val contextThemeWrapper = ContextThemeWrapper(
            this,
            styleRes
        )
        val textView = TextView(contextThemeWrapper)
         */

        val textView = TextView(this)
        textView.setTextAppearance(styleRes)
        textView.text = text
        return textView
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////

    private fun saveData(data: String) {
        saveDataSQLite(data)
        saveDataPreferences(data)
        saveDataInternalFilesDirectory(data)
        saveDataInternalCacheDirectory(data)
        saveDataExternalFilesDirectory(data)
        saveDataExternalCacheDirectory(data)
        saveDataExternalStoragePublicDirectoryWithScopeProvider(data)
        saveDataRoom(data)
        saveDataContentProvider(data)
    }

    private fun saveDataPreferences(data: String) {
        val editor = sharedPreferences.edit()
        editor.putString("myData", data)
        editor.apply() // or editor.commit();
    }

    private fun saveDataInternalFilesDirectory(data: String) {
        val file: File = File(getFilesDir(), DATA_FILE_NAME)
        writeFileData(file, data)
    }

    private fun saveDataInternalCacheDirectory(data: String) {
        val file: File = File(getCacheDir(), DATA_FILE_NAME)
        writeFileData(file, data)
    }

    private fun saveDataExternalFilesDirectory(data: String) {
        val file: File = File(getExternalFilesDir(null), DATA_FILE_NAME)
        writeFileData(file, data)
    }

    private fun saveDataExternalCacheDirectory(data: String) {
        val file: File = File(getExternalCacheDir(), DATA_FILE_NAME)
        writeFileData(file, data)
    }

    var recentlyDownloadFile: Uri? = null

    private fun saveDataExternalStoragePublicDirectoryWithScopeProvider(data: String) {
        val resolver = contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, DATA_FILE_NAME)
            put(MediaStore.MediaColumns.MIME_TYPE, "text/plain")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        }

        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
        uri?.let {
            var outputStream: OutputStream? = null
            try {
                outputStream = resolver.openOutputStream(it)
                outputStream?.write(data.toByteArray())
                Log.d(
                    "WRITEFILE",
                    "Write file in external storage downloads public directory using scoped storage provider success"
                )
                recentlyDownloadFile = uri
            } catch (e: IOException) {
                Log.e(
                    "WRITEFILE",
                    "Unable to write file in external storage downloads public directory using scoped storage provider"
                )
                e.printStackTrace()
            } finally {
                try {
                    outputStream?.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        } ?: run {
            Log.e(
                "WRITEFILE",
                "Unable to write file in external storage downloads public directory using scoped storage provider"
            )
        }
    }

    private fun saveDataSQLite(data: String) {
        dbHelper.addUser(data, 0)
    }

    private fun saveDataRoom(data: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val myData = MyData()
            myData.data = data
            myDataDao.insertOrUpdateUser(myData)
        }
    }

    private fun saveDataContentProvider(data: String) {
        val values = ContentValues()
        values.put("data", data)
        val newUri = contentResolver.insert(
            MyDataProvider.CONTENT_URI,
            values
        )
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////

    private fun readData() {
        findViewById<LinearLayout>(R.id.listAll).removeAllViews()
        findViewById<LinearLayout>(R.id.listAll).removeAllViewsInLayout()

        readDataSQLite()
        readDataFromPreferences()
        readDataFromInternalFilesDirectory()
        readDataFromInternalCacheDirectory()
        readDataFromExternalFilesDirectory()
        readDataFromExternalCacheDirectory()
        readDataExternalStoragePublicDirectoryScopedProvider()
        readDataRoom()
        readDataContentProvider()
    }

    private fun readDataFromPreferences() {
        renderReadData(
            title = "Shared preferences",
            command = "sharedPreferences.getString(\"myData\", \"\")",
            data = sharedPreferences.getString("myData", "") ?: "",
            path = " /data/data/${packageName}/shared_prefs/"
        )
    }

    private fun readDataFromInternalFilesDirectory() {
        val file: File = File(getFilesDir(), DATA_FILE_NAME)
        renderReadData(
            title = "Internal storage files directory",
            command = "getFilesDir()",
            data = readFileData(file),
            path = file.path
        )
    }

    private fun readDataFromInternalCacheDirectory() {
        val file: File = File(getCacheDir(), DATA_FILE_NAME)
        renderReadData(
            title = "Internal storage cache directory",
            command = "getCacheDir()",
            data = readFileData(file),
            path = file.path
        )
    }

    private fun readDataFromExternalFilesDirectory() {
        val file: File = File(getExternalFilesDir(null), DATA_FILE_NAME)
        renderReadData(
            title = "External storage files directory",
            command = "getExternalFilesDir(null)",
            data = readFileData(file),
            path = file.path
        )
    }

    private fun readDataFromExternalCacheDirectory() {
        val file: File = File(getExternalCacheDir(), DATA_FILE_NAME)
        renderReadData(
            title = "External storage cache directory",
            command = "getExternalCacheDir()",
            data = readFileData(file),
            path = file.path
        )
    }

    private fun readDataExternalStoragePublicDirectoryScopedProvider() {
        var data = ""
        var filePath: String? = ""
        val resolver = contentResolver

        recentlyDownloadFile?.let {
            filePath = it.path
            resolver.openInputStream(it)?.use { inputStream ->
                data = inputStream.bufferedReader().use { it.readText() }
            }
        } ?: {
            val projection =
                arrayOf(MediaStore.MediaColumns.DISPLAY_NAME, MediaStore.MediaColumns._ID)
            val selection = "${MediaStore.MediaColumns.DISPLAY_NAME}=?"
            val selectionArgs = arrayOf(DATA_FILE_NAME)
            val contentUri = MediaStore.Downloads.EXTERNAL_CONTENT_URI

            val cursor = resolver.query(contentUri, projection, selection, selectionArgs, null)
            cursor?.use {
                val idColumn = it.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                if (it.moveToFirst()) {
                    val id = it.getLong(idColumn)
                    val fileUri = android.net.Uri.withAppendedPath(contentUri, id.toString())
                    filePath = fileUri.path
                    resolver.openInputStream(fileUri)?.use { inputStream ->
                        data = inputStream.bufferedReader().use { it.readText() }
                    }
                } else {
                    Log.e(
                        "READFILE",
                        "File not found in external storage downloads public directory"
                    )
                }
            }
        }

        renderReadData(
            title = "External storage downloads (public) directory using scoped storage provider",
            command = "val cursor = resolver.query(MediaStore.Downloads.EXTERNAL_CONTENT_URI, arrayOf(MediaStore.MediaColumns.DISPLAY_NAME, MediaStore.MediaColumns._ID), MediaStore.MediaColumns.DISPLAY_NAME, DATA_FILE_NAME, null)",
            data = data,
            path = filePath
        )
    }

    private fun readDataSQLite() {
        val user = dbHelper.getUser()
        val stringBuffer = StringBuffer()
        user?.let {
            stringBuffer.append(it.name)
        }
        renderReadData(
            title = "SQLite",
            command = "dbHelper.getAllUsers()",
            data = stringBuffer.toString(),
            path = "/data/data/${DBHelper.DATABASE_NAME}/databases"
        )
    }

    private fun readDataRoom() {
        CoroutineScope(Dispatchers.IO).launch {
            val myData = myDataDao.loadUser()
            withContext(Dispatchers.Main) {
                renderReadData(
                    title = "Room",
                    command = "myDataDao.loadById(1)",
                    data = myData?.data ?: "",
                    path = getDatabasePath(ROOM_DB_NAME).getAbsolutePath()
                )
            }
        }
    }

    private fun readDataContentProvider() {
        var myData = ""
        val cursor = contentResolver.query(
            MyDataProvider.CONTENT_URI,
            null, null, null, null
        )
        if (cursor != null && cursor.moveToFirst()) {
            myData = cursor.getString(cursor.getColumnIndexOrThrow("data"))
            cursor.close()
        }
        renderReadData(
            title = "Content Provider",
            command = "contentResolver.query( MyDataProvider.CONTENT_URI, null, null, null, null)",
            data = myData,
            //path = PathUtil.getPath(this, MyDataProvider.CONTENT_URI),
            path = MyDataProvider.CONTENT_URI.path
        )
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////

    private fun writeFileData(file: File, data: String) {
        try {
            FileOutputStream(file).use { fos ->
                fos.write(data.toByteArray())
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun readFileData(file: File): String {
        val stringBuilder = StringBuilder()
        try {
            FileInputStream(file).use { fis ->
                val inputStreamReader = InputStreamReader(fis)
                val bufferedReader = BufferedReader(inputStreamReader)
                var line: String?
                while ((bufferedReader.readLine().also { line = it }) != null) {
                    stringBuilder.append(line).append('\n')
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return stringBuilder.toString()
    }

    companion object {
        val DATA_FILE_NAME = "myData.txt"
        val ROOM_DB_NAME = "my-database-name"
    }
}