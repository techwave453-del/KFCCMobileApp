package com.example.helloworld.admin.content

import com.example.helloworld.admin.AdminRepository
import com.example.helloworld.data.ChurchInfo

class WebsiteContentRepository(private val adminRepository: AdminRepository) {
    suspend fun load(): Result<ChurchInfo> = adminRepository.loadSiteContent()

    suspend fun save(
        content: ChurchInfo,
        canEditIdentity: Boolean = false,
        canManageLive: Boolean = false
    ): Result<ChurchInfo> =
        adminRepository.saveSiteContent(content, canEditIdentity, canManageLive)

    suspend fun saveLiveStream(
        liveStream: com.example.helloworld.data.LiveStream
    ): Result<Unit> = adminRepository.saveLiveStream(liveStream)
}
