package com.example.helloworld.admin.content

import com.example.helloworld.admin.AdminRepository
import com.example.helloworld.data.ChurchInfo

class WebsiteContentRepository(private val adminRepository: AdminRepository) {
    suspend fun load(): Result<ChurchInfo> = adminRepository.loadSiteContent()

    suspend fun save(content: ChurchInfo): Result<ChurchInfo> = adminRepository.saveSiteContent(content)
}
