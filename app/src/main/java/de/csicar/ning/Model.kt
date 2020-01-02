package de.csicar.ning

import android.util.Log
import androidx.room.DatabaseView
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import de.csicar.ning.scanner.MacAddress
import java.net.Inet4Address

@Entity
data class Scan(@PrimaryKey(autoGenerate = true) val scanId: Long, val startedAt: Long)

@Entity
data class Device(
    @PrimaryKey(autoGenerate = true) val deviceId: Long,
    val networkId: Long,
    val ip: Inet4Address,
    val deviceName: String?,
    val hwAddress: MacAddress?
) {


}

@DatabaseView("SELECT Device.deviceId, Device.networkId, Device.ip, Device.hwAddress, Device.deviceName, MacVendor.name as vendorName FROM Device LEFT JOIN MacVendor ON MacVendor.mac = substr(Device.hwAddress, 0, 9)")
data class DeviceWithName(
    val deviceId: Long, val networkId: Long, val ip: Inet4Address, val hwAddress: MacAddress?
    , val deviceName: String?
    , val vendorName: String?
) {
    @Ignore
    val asDevice = Device(deviceId, networkId, ip, deviceName, hwAddress)
}

@Entity
data class Network(
    @PrimaryKey(autoGenerate = true) val networkId: Long,
    val baseIp: Inet4Address,
    val mask: Short,
    val scanId: Long,
    val interfaceName: String
) {
    companion object {
        fun from(ip: Inet4Address, mask: Short, scanId: Long, interfaceName: String): Network {
            return Network(0, ip.maskWith(mask), mask, scanId, interfaceName)
        }
    }

    fun enumerateAddresses(): Sequence<Inet4Address> {
        return generateSequence(0) {
            val next = it + 1
            if (next < networkSize) next else null
        }
            .map { baseIp.hashCode() + it }
            .map { inet4AddressFromInt("", it) }
    }

    fun containsAddress(host: Inet4Address): Boolean {
        return this.baseIp.maskWith(mask) == host.maskWith(mask)
    }

    val networkSize get() = 2.shl(32 - mask.toInt() - 1)

}

enum class Protocol {
    TCP, UDP
}

@Entity
data class Port(
    @PrimaryKey(autoGenerate = true) val portId: Long, val port: Int,
    val protocol: Protocol,
    val deviceId: Long
) {
    val description get() = PortDescription.commonPorts.find { it.port == port }
}

@Entity(primaryKeys = ["name", "mac"])
data class MacVendor(val name: String, val mac: String)

@Entity
data class PortDescription(
    @PrimaryKey
    val portId: Long,
    val port: Int,
    val protocol: Protocol,
    val serviceName: String,
    val serviceDescription: String
) {
    companion object {
        val commonPorts = listOf(
            PortDescription(0, 21, Protocol.TCP, "FTP", "File Transfer Protocol"),
            PortDescription(0, 22, Protocol.TCP, "SFTP", "Secure FTP"),
            PortDescription(0, 80, Protocol.TCP, "HTTP", "Hypertext Transport Protocol"),
            PortDescription(0, 53, Protocol.UDP, "DNS", "DNS Server"),
            PortDescription(0, 443, Protocol.TCP, "HTTPS", "Secure HTTP"),
            PortDescription(0, 548, Protocol.TCP, "AFP", "AFP over TCP"),
            PortDescription(0, 8080, Protocol.TCP, "HTTP-Proxy", "HTTP Proxy"),
            PortDescription(0, 62078, Protocol.TCP, "iPhone-Sync", "lockdown iOS Service")
        )
    }
}