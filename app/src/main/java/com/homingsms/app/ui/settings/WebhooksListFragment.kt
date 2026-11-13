package com.homingsms.app.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.homingsms.app.databinding.FragmentWebhooksListBinding
import com.homingsms.app.modules.status.AppStateRepository
import com.homingsms.app.ui.adapters.WebhookAdapter
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class WebhooksListFragment : Fragment() {
    private var _binding: FragmentWebhooksListBinding? = null
    private val binding get() = requireNotNull(_binding)

    private val state: AppStateRepository by inject()
    private val adapter = WebhookAdapter()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentWebhooksListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.webhookList.layoutManager = LinearLayoutManager(requireContext())
        binding.webhookList.adapter = adapter
        binding.webhookList.addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                state.webhookEndpoints.collect { endpoints ->
                    adapter.submitList(endpoints)
                    binding.emptyState.isVisible = endpoints.isEmpty()
                    binding.webhookList.isVisible = endpoints.isNotEmpty()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
