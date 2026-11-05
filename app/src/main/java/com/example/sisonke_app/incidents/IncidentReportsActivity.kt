package com.example.sisonke_app.incidents

import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.sisonke_app.R
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import java.text.DateFormat
import java.util.Date

class IncidentReportsActivity : AppCompatActivity() {
    private lateinit var list: LinearLayout
    private lateinit var status: TextView
    private var registration: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_incident_reports)
        list = findViewById(R.id.incidentList)
        status = findViewById(R.id.tvReportsStatus)
        findViewById<View>(R.id.btnReportsBack).setOnClickListener { finish() }
    }

    override fun onStart() {
        super.onStart()
        registration = FirebaseFirestore.getInstance()
            .collection("incidents")
            .orderBy("dateTime", Query.Direction.DESCENDING)
            .addSnapshotListener(this) { snapshot, error ->
                if (error != null) {
                    status.text = getString(R.string.incidents_load_error)
                    return@addSnapshotListener
                }
                if (snapshot == null) return@addSnapshotListener
                list.removeAllViews()
                status.text = resources.getQuantityString(
                    R.plurals.incident_report_count, snapshot.size(), snapshot.size()
                )
                if (snapshot.isEmpty) {
                    val empty = TextView(this).apply {
                        text = getString(R.string.no_incident_reports)
                        textSize = 15f
                        gravity = Gravity.CENTER
                        setPadding(24, 48, 24, 48)
                    }
                    list.addView(empty)
                    return@addSnapshotListener
                }
                snapshot.documents.forEach { doc ->
                    val card = LinearLayout(this).apply {
                        orientation = LinearLayout.VERTICAL
                        setPadding(18, 14, 18, 14)
                        setBackgroundResource(R.drawable.bg_card_rounded)
                        val params = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply { bottomMargin = 12 }
                        layoutParams = params
                    }
                    val category = doc.getString("category").orEmpty().ifBlank { getString(R.string.unknown_category) }
                    val description = doc.getString("description").orEmpty()
                    val id = doc.getString("id") ?: doc.id
                    val timestamp = doc.getLong("dateTime") ?: 0L
                    val date = if (timestamp > 0) DateFormat.getDateTimeInstance().format(Date(timestamp))
                               else getString(R.string.date_unavailable)
                    val userId = doc.getString("userId").orEmpty()
                    val location = if (doc.getDouble("latitude") != null && doc.getDouble("longitude") != null &&
                        (doc.getDouble("latitude") != 0.0 || doc.getDouble("longitude") != 0.0)) {
                        "%.5f, %.5f".format(doc.getDouble("latitude"), doc.getDouble("longitude"))
                    } else getString(R.string.location_not_provided)

                    card.addView(TextView(this).apply {
                        text = "$category  •  $id"
                        textSize = 16f
                        setTextColor(getColor(R.color.sisonke_primary))
                        setTypeface(typeface, android.graphics.Typeface.BOLD)
                    })
                    card.addView(TextView(this).apply {
                        text = description
                        textSize = 14f
                        setPadding(0, 8, 0, 8)
                    })
                    card.addView(TextView(this).apply {
                        text = "${getString(R.string.reported_on)}: $date\n${getString(R.string.reported_by)}: ${userId.ifBlank { getString(R.string.unknown_user) }}\n${getString(R.string.location)}: $location"
                        textSize = 12f
                        setTextColor(getColor(R.color.sisonke_grey))
                    })
                    val imageUrl = doc.getString("imageUrl").orEmpty()
                    if (imageUrl.isNotBlank()) {
                        card.addView(TextView(this).apply {
                            text = getString(R.string.photo_attached)
                            textSize = 12f
                            setPadding(0, 8, 0, 0)
                        })
                    }
                    list.addView(card)
                }
            }
    }

    override fun onStop() {
        registration?.remove()
        registration = null
        super.onStop()
    }
}
