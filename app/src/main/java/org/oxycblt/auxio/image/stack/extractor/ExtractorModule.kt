/*
 * Copyright (c) 2024 Auxio Project
 * ExtractorModule.kt is part of Auxio.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
 
package org.oxycblt.auxio.image.stack.extractor

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
interface ExtractorModule {
    @Binds fun coverExtractor(impl: CoverExtractorImpl): CoverExtractor
}

@Module
@InstallIn(SingletonComponent::class)
class CoverSourcesModule {
    @Provides
    fun coverSources(exoPlayerCoverSource: ExoPlayerCoverSource, aospCoverSource: AOSPCoverSource) =
        CoverSources(listOf(aospCoverSource, exoPlayerCoverSource))
}
