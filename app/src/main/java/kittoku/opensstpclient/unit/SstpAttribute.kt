package kittoku.opensstpclient.unit

import kittoku.opensstpclient.misc.IncomingBuffer
import kittoku.opensstpclient.misc.generateResolver
import java.nio.ByteBuffer
import kotlin.math.min


internal enum class AttributeId(val value: Byte) {
    NO_ERROR(0),
    ENCAPSULATED_PROTOCOL_ID(1),
    STATUS_INFO(2),
    CRYPTO_BINDING(3),
    CRYPTO_BINDING_REQ(4);

    companion object {
        internal val resolve = generateResolver(values(), AttributeId::value)
    }
}

internal enum class HashProtocol(val value: Byte) {
    CERT_HASH_PROTOCOL_SHA1(1),
    CERT_HASH_PROTOCOL_SHA256(2);

    companion object {
        internal val resolve = generateResolver(values(), HashProtocol::value)
    }
}

internal abstract class Attribute : ShortLengthDataUnit() {
    internal abstract val id: Byte

    override val validLengthRange = 4..Short.MAX_VALUE

    internal fun readHeader(bytes: IncomingBuffer) { setTypedLength(bytes.getShort()) }

    internal fun writeHeader(bytes: ByteBuffer) {
        bytes.put(0)
        bytes.put(id)
        bytes.putShort(getTypedLength())
    }
}

internal class EncapsulatedProtocolId : Attribute() {
    override val id = AttributeId.ENCAPSULATED_PROTOCOL_ID.value

    override val validLengthRange = 6..6

    internal var protocolId: Short = 1

    override fun read(bytes: IncomingBuffer) {
        readHeader(bytes)
        protocolId = bytes.getShort()
    }

    override fun write(bytes: ByteBuffer) {
        writeHeader(bytes)
        bytes.putShort(protocolId)
    }

    override fun update() { _length = validLengthRange.first }
}

internal class StatusInfo : Attribute() {
    override val id = AttributeId.STATUS_INFO.value

    override val validLengthRange = 12..Short.MAX_VALUE

    internal var targetId: Byte = 0

    internal var status: Int = 0

    internal val holder = mutableListOf<Byte>()

    override fun read(bytes: IncomingBuffer) {
        readHeader(bytes)
        bytes.move(3)
        targetId = bytes.getByte()
        status = bytes.getInt()
        repeat(_length - validLengthRange.first) { holder.add(bytes.getByte()) }
    }

    override fun write(bytes: ByteBuffer) {
        writeHeader(bytes)
        repeat(3) { bytes.put(0) }
        bytes.put(targetId)
        bytes.putInt(status)
        holder.slice(0..min(holder.lastIndex, 63)).forEach { bytes.put(it) }
    }

    override fun update() {
        _length = validLengthRange.first + min(holder.size, 64)
    }
}

internal class CryptoBinding : Attribute() {
    override val id = AttributeId.CRYPTO_BINDING.value

    override val validLengthRange = 104..104

    internal var hashProtocol: Byte = 2

    internal val nonce = ByteArray(32)

    internal val certHash = ByteArray(32)

    internal val compoundMac = ByteArray(32)

    override fun read(bytes: IncomingBuffer) {
        readHeader(bytes)
        bytes.move(3)
        hashProtocol = bytes.getByte()
        repeat(32) { nonce[it] = bytes.getByte() }
        repeat(32) { certHash[it] = bytes.getByte() }
        repeat(32) { compoundMac[it] = bytes.getByte() }
    }

    override fun write(bytes: ByteBuffer) {
        writeHeader(bytes)
        repeat(3) { bytes.put(0) }
        bytes.put(hashProtocol)
        bytes.put(nonce)
        bytes.put(certHash)
        bytes.put(compoundMac)
    }

    override fun update() { _length = validLengthRange.first }
}

internal class CryptoBindingRequest : Attribute() {
    override val id = AttributeId.CRYPTO_BINDING_REQ.value

    override val validLengthRange = 40..40

    internal var bitmask: Byte = 3

    internal val nonce = ByteArray(32)

    override fun read(bytes: IncomingBuffer) {
        readHeader(bytes)
        bytes.move(3)
        bitmask = bytes.getByte()
        repeat(32) { nonce[it] = bytes.getByte() }
    }

    override fun write(bytes: ByteBuffer) {
        writeHeader(bytes)
        repeat(3) { bytes.put(0) }
        bytes.put(bitmask)
        bytes.put(nonce)
    }

    override fun update() { _length = validLengthRange.first }
}
