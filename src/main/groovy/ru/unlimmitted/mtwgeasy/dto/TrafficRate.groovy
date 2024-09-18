package ru.unlimmitted.mtwgeasy.dto

import com.fasterxml.jackson.databind.annotation.JsonDeserialize

import java.time.Instant

@JsonDeserialize(builder = Builder.class)
class TrafficRate {
	Long tx
	Long rx
	Long time

	TrafficRate(Long tx, Long rx, Instant time) {
		this.tx = tx
		this.rx = rx
		this.time = time.getEpochSecond()
	}

	static class Builder {
		TrafficRate rate
		Long tx
		Long rx
		Long time

		Builder tx(Long tx) {
			this.tx = tx
			return this
		}

		Builder rx(Long rx) {
			this.rx = rx
			return this
		}

		Builder time(Long time) {
			this.time = time
			return this
		}

		TrafficRate build() {
			return new TrafficRate(tx, rx, time)
		}
	}

}
