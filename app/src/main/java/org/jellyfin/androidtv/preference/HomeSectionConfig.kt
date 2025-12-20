package org.jellyfin.androidtv.preference

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.jellyfin.androidtv.constant.HomeSectionType

/**
 * Custom serializer for HomeSectionType enum.
 */
object HomeSectionTypeSerializer : KSerializer<HomeSectionType> {
	override val descriptor: SerialDescriptor = 
		PrimitiveSerialDescriptor("HomeSectionType", PrimitiveKind.STRING)

	override fun serialize(encoder: Encoder, value: HomeSectionType) {
		encoder.encodeString(value.serializedName)
	}

	override fun deserialize(decoder: Decoder): HomeSectionType {
		val name = decoder.decodeString()
		return HomeSectionType.entries.find { it.serializedName == name } 
			?: HomeSectionType.NONE
	}
}

/**
 * Configuration for a home section row.
 */
@Serializable
data class HomeSectionConfig(
	@Serializable(with = HomeSectionTypeSerializer::class)
	val type: HomeSectionType,
	val enabled: Boolean = true,
	val order: Int = 0,
) {
	companion object {
		/**
		 * Default home sections configuration
		 */
		fun defaults(): List<HomeSectionConfig> = listOf(
			HomeSectionConfig(HomeSectionType.MEDIA_BAR, enabled = true, order = 0),
			HomeSectionConfig(HomeSectionType.RESUME, enabled = true, order = 1),
			HomeSectionConfig(HomeSectionType.NEXT_UP, enabled = true, order = 2),
			HomeSectionConfig(HomeSectionType.LIVE_TV, enabled = true, order = 3),
			HomeSectionConfig(HomeSectionType.LATEST_MEDIA, enabled = true, order = 4),
			HomeSectionConfig(HomeSectionType.LIBRARY_TILES_SMALL, enabled = false, order = 5),
			HomeSectionConfig(HomeSectionType.LIBRARY_BUTTONS, enabled = false, order = 6),
			HomeSectionConfig(HomeSectionType.RESUME_AUDIO, enabled = false, order = 7),
			HomeSectionConfig(HomeSectionType.RESUME_BOOK, enabled = false, order = 8),
			HomeSectionConfig(HomeSectionType.ACTIVE_RECORDINGS, enabled = false, order = 9),
			HomeSectionConfig(HomeSectionType.PLAYLIST, enabled = false, order = 10),
		)
	}
}
