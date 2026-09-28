package com.investimentoeasy.core.database

import com.investimentoeasy.core.domain.mercado.RepositorioDeMercado
import com.investimentoeasy.core.model.PanoramaMercado
import kotlinx.serialization.SerializationException
import javax.inject.Inject

internal class RoomRepositorioDeMercado
    @Inject
    constructor(
        private val dao: MercadoDao,
    ) : RepositorioDeMercado {
        override suspend fun salvar(panorama: PanoramaMercado) {
            dao.gravar(MercadoEntity(obtidoEm = panorama.obtidoEm.toEpochMilli(), conteudo = MercadoJson.de(panorama).texto()))
        }

        /** Cache ilegível (formato antigo) conta como "sem cache": basta atualizar de novo. */
        override suspend fun ultimo(): PanoramaMercado? =
            dao.ultimo()?.let {
                try {
                    MercadoJson.de(it.conteudo).paraModelo()
                } catch (_: SerializationException) {
                    null
                } catch (_: IllegalArgumentException) {
                    null
                }
            }
    }
