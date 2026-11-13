package com.homingsms.app.ui.adapters

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.homingsms.app.R
import com.homingsms.app.databinding.ItemWebhookBinding
import com.homingsms.core.model.WebhookEndpoint

class WebhookAdapter : ListAdapter<WebhookEndpoint, WebhookAdapter.ViewHolder>(WebhookDiffCallback()) {
    class ViewHolder(private val binding: ItemWebhookBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(webhook: WebhookEndpoint) {
            val context = binding.root.context
            binding.idText.text = context.getString(R.string.webhook_id_format, webhook.id)
            binding.urlText.text = webhook.url
            binding.eventText.text = webhook.events.joinToString { it.value }
            binding.sourceText.text = context.getString(if (webhook.enabled) R.string.enabled else R.string.disabled)
            binding.sourceIcon.setImageResource(R.drawable.ic_local_server)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemWebhookBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        val holder = ViewHolder(binding)
        binding.root.setOnClickListener {
            val position = holder.bindingAdapterPosition
            if (position == RecyclerView.NO_POSITION) return@setOnClickListener
            val context = binding.root.context
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("Webhook ID", getItem(position).id))
            Toast.makeText(context, R.string.id_copied, Toast.LENGTH_SHORT).show()
        }
        return holder
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class WebhookDiffCallback : DiffUtil.ItemCallback<WebhookEndpoint>() {
        override fun areItemsTheSame(oldItem: WebhookEndpoint, newItem: WebhookEndpoint): Boolean = oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: WebhookEndpoint, newItem: WebhookEndpoint): Boolean = oldItem == newItem
    }
}
