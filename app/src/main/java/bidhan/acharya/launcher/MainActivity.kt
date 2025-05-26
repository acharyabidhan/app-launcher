package bidhan.acharya.launcher

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

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