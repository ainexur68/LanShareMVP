package com.example.lanshare

import android.content.Context
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import java.net.Inet4Address

object NetworkAddress {
    fun localIpv4(context: Context): String? {
        val connectivity = context.applicationContext
            .getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val candidates = buildList {
            connectivity.activeNetwork?.let(::add)
            connectivity.allNetworks.forEach { network ->
                if (network != connectivity.activeNetwork) add(network)
            }
        }

        return candidates.firstNotNullOfOrNull { network ->
            if (!isLanNetwork(connectivity.getNetworkCapabilities(network))) return@firstNotNullOfOrNull null
            ipv4(connectivity.getLinkProperties(network))
        }
    }

    fun observe(context: Context, onChanged: (String?) -> Unit): () -> Unit {
        val appContext = context.applicationContext
        val connectivity = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val request = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .addTransportType(NetworkCapabilities.TRANSPORT_ETHERNET)
            .build()

        val callback = object : ConnectivityManager.NetworkCallback() {
            private fun publish() = onChanged(localIpv4(appContext))

            override fun onAvailable(network: Network) = publish()
            override fun onLost(network: Network) = publish()
            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) = publish()
            override fun onLinkPropertiesChanged(network: Network, linkProperties: LinkProperties) = publish()
        }

        connectivity.registerNetworkCallback(request, callback)
        onChanged(localIpv4(appContext))

        return {
            runCatching { connectivity.unregisterNetworkCallback(callback) }
        }
    }

    private fun isLanNetwork(capabilities: NetworkCapabilities?): Boolean =
        capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true ||
            capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) == true

    private fun ipv4(linkProperties: LinkProperties?): String? =
        linkProperties?.linkAddresses
            ?.asSequence()
            ?.map { it.address }
            ?.filterIsInstance<Inet4Address>()
            ?.firstOrNull { it.isSiteLocalAddress && !it.isLoopbackAddress }
            ?.hostAddress
}
