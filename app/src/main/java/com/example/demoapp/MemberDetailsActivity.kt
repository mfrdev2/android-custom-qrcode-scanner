package com.example.demoapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MemberDetailsActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: MemberAdapter
    private var memberList = mutableListOf<Member>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_member_details)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }



        setupViews()
        setupToolbar()

        val qrCode = intent.getStringExtra("qr_code")
        qrCode?.let {
            loadMemberData(it)
        }
    }

    private fun setupViews() {
        recyclerView = findViewById(R.id.recyclerViewMembers)
        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = MemberAdapter(memberList) { member ->
            // Handle member item click
            showMemberActions(member)
        }
        recyclerView.adapter = adapter
    }

    private fun setupToolbar() {
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = "Member Details"
        }
    }

    private fun loadMemberData(qrCode: String) {
        // Simulate loading member data from QR code
        // In real app, you would decode QR code and fetch data from API

        memberList.clear()
        memberList.addAll(getMockMemberData(qrCode))
        adapter.notifyDataSetChanged()

        // Update header info
        val headerName = findViewById<TextView>(R.id.headerName)
        val headerTicket = findViewById<TextView>(R.id.headerTicket)
        val headerVenue = findViewById<TextView>(R.id.headerVenue)
        val headerDateTime = findViewById<TextView>(R.id.headerDateTime)

        if (memberList.isNotEmpty()) {
            val primaryMember = memberList[0]
            headerName.text = primaryMember.name
            headerTicket.text = "Ticket No: ${primaryMember.ticketNo}"
            headerVenue.text = "Venue: SA Islamic Resource Center New Town Dhaka"
            headerDateTime.text = "Event Time: Friday 28th December from 9:00 AM - 9:00 PM"
        }
    }

    private fun getMockMemberData(qrCode: String): List<Member> {
        // Mock data based on your screenshot
        return listOf(
            Member("Nazmul Huda Nazmul", "TT5397", "Father's", R.drawable.ic_qr_code, true),
            Member("Jakiral Islam Huda", "TT5397", "Father's", R.drawable.ic_qr_code, false),
            Member("Sara Ahmad", "TT5396", "Mother's", R.drawable.ic_qr_code, false),
            Member("Anik Rahman", "TT5396", "Son", R.drawable.ic_qr_code, false),
            Member("Fatima Noor", "TT5400", "Sister", R.drawable.ic_qr_code, false),
            Member("Raza Khan", "TT5398", "Brother", R.drawable.ic_qr_code, false),
            Member("Laila Sultana", "TT5402", "Sister", R.drawable.ic_qr_code, true)
        )
    }

    private fun showMemberActions(member: Member) {
        val options = arrayOf("View Details", "Mark Attended", "Remove from List")

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(member.name)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showMemberDetails(member)
                    1 -> markAttended(member)
                    2 -> removeMember(member)
                }
            }
            .show()
    }

    private fun showMemberDetails(member: Member) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Member Details")
            .setMessage("Name: ${member.name}\nTicket: ${member.ticketNo}\nRelation: ${member.relation}")
            .setPositiveButton("OK", null)
            .show()
    }

    private fun markAttended(member: Member) {
        val index = memberList.indexOf(member)
        if (index != -1) {
            memberList[index] = member.copy(isHighlighted = !member.isHighlighted)
            adapter.notifyItemChanged(index)
        }
    }

    private fun removeMember(member: Member) {
        val index = memberList.indexOf(member)
        if (index != -1) {
            memberList.removeAt(index)
            adapter.notifyItemRemoved(index)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

}

// Member.kt - Data class
data class Member(
    val name: String,
    val ticketNo: String,
    val relation: String,
    val avatarRes: Int,
    val isHighlighted: Boolean = false
)
// MemberAdapter.kt - RecyclerView Adapter
class MemberAdapter(
    private val members: List<Member>,
    private val onMemberClick: (Member) -> Unit
) : RecyclerView.Adapter<MemberAdapter.MemberViewHolder>() {

    class MemberViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val avatar: ImageView = itemView.findViewById(R.id.memberAvatar)
        val name: TextView = itemView.findViewById(R.id.memberName)
        val ticketNo: TextView = itemView.findViewById(R.id.memberTicketNo)
        val relation: TextView = itemView.findViewById(R.id.memberRelation)
        val container: View = itemView.findViewById(R.id.memberContainer)
        val statusIndicator: View = itemView.findViewById(R.id.statusIndicator)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MemberViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_member, parent, false)
        return MemberViewHolder(view)
    }

    override fun onBindViewHolder(holder: MemberViewHolder, position: Int) {
        val member = members[position]

        holder.name.text = member.name
        holder.ticketNo.text = "Ticket No: ${member.ticketNo}"
        holder.relation.text = member.relation
        holder.avatar.setImageResource(member.avatarRes)

        // Highlight effect
        if (member.isHighlighted) {
            holder.container.setBackgroundColor(
                ContextCompat.getColor(holder.itemView.context, android.R.color.holo_green_light)
            )
            holder.statusIndicator.visibility = View.VISIBLE
        } else {
            holder.container.setBackgroundColor(
                ContextCompat.getColor(holder.itemView.context, android.R.color.white)
            )
            holder.statusIndicator.visibility = View.GONE
        }

        holder.itemView.setOnClickListener {
            onMemberClick(member)
        }
    }

    override fun getItemCount() = members.size
}