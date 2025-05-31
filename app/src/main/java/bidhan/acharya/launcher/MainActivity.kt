package bidhan.acharya.launcher

import android.app.AlertDialog
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import java.io.File

class MainActivity : AppCompatActivity() {

    data class AppInfo(
        val name: String,
        val packageName: String,
        val icon: Drawable
    )

    private val appList: MutableList<AppInfo> = mutableListOf()
    private lateinit var appListViewAdapter: AppListViewAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val appListView = findViewById<ListView>(R.id.appListView)
        appList.addAll(getApps().sortedBy { it.name.lowercase() })
        appListViewAdapter = AppListViewAdapter(this, appList)
        appListView.adapter = appListViewAdapter
        appListView.setOnItemClickListener { _, _, position, _ ->
            val clickedApp = appList[position]
            val launchIntent = packageManager.getLaunchIntentForPackage(clickedApp.packageName)
            if (launchIntent != null)
                startActivity(launchIntent)
            else
                Toast.makeText(this, "App not found or has no launcher activity", Toast.LENGTH_SHORT).show()
        }
        appListView.setOnItemLongClickListener { _, _, position, _ ->
            showColorPickerDialog(appList[position])
            true
        }
        appListViewAdapter.notifyDataSetChanged()
    }

    private fun getApps(): List<AppInfo> {
        val pm: PackageManager = packageManager
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val apps = pm.queryIntentActivities(intent, 0)
        return apps.map { resolveInfo ->
            AppInfo(
                name = resolveInfo.loadLabel(pm).toString(),
                packageName = resolveInfo.activityInfo.packageName,
                icon = resolveInfo.loadIcon(pm)
            )
        }
    }

    private fun showColorPickerDialog(app: AppInfo) {
        val inflater: LayoutInflater = LayoutInflater.from(this)
        val appOtpView = inflater.inflate(R.layout.app_option_dialog, null)
        val builder = AlertDialog.Builder(this)
        builder.setTitle(app.name).setView(appOtpView).setNegativeButton("Close") { d, _ -> d.dismiss() }
        val dialog: AlertDialog = builder.create()
        dialog.show()
        appOtpView.findViewById<Button>(R.id.appInfoBtn).setOnClickListener {
            dialog.dismiss()
            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = "package:${app.packageName}".toUri()
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            })
        }
        appOtpView.findViewById<Button>(R.id.apkShareBtn).setOnClickListener {
            dialog.dismiss()
            shareAppApk(app.packageName)
        }
    }

    private fun shareApkFileSecurely(apkFile: File) {
        val uri = FileProvider.getUriForFile(
            this,
            "${packageName}.fileprovider",
            apkFile
        )
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "*/*"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            // 👇 This ensures proper URI access for all apps
            clipData = ClipData.newUri(contentResolver, "APK", uri)
        }
        startActivity(Intent.createChooser(shareIntent, "Share APK"))
    }

    private fun shareAppApk(targetPackage: String) {
        try {
            val appInfo = packageManager.getApplicationInfo(targetPackage, 0)
            val sourceApk = File(appInfo.sourceDir)
            val destDir = File(cacheDir, "apks").apply { mkdirs() }
            val destFile = File(destDir, "$targetPackage.apk")
            // Copy the APK
            sourceApk.inputStream().use { input ->
                destFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            shareApkFileSecurely(destFile)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Failed to share APK", Toast.LENGTH_SHORT).show()
        }
    }

}

class AppListViewAdapter(
    private val context: Context,
    items: MutableList<MainActivity.AppInfo>,
) : ArrayAdapter<MainActivity.AppInfo>(context, 0, items) {
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val itemView = convertView ?: LayoutInflater.from(context).inflate(R.layout.app_list_item, parent, false)
        val item = getItem(position)
        val appIcon = itemView.findViewById<ImageView>(R.id.appIcon)
        val appName = itemView.findViewById<TextView>(R.id.appName)
        val appPackage = itemView.findViewById<TextView>(R.id.appPackage)
        if (item != null) {
           appIcon.setImageDrawable(item.icon)
            appName.text = item.name
            appPackage.text = item.packageName
        }
        return itemView
    }
}