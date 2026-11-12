package br.com.regulacheck.auth;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

/** Relogio de teste que so anda quando o teste manda, para expirar sessoes de forma deterministica. */
public final class RelogioFixo extends Clock {

    private Instant agora;

    /** @param inicio instante inicial */
    public RelogioFixo(Instant inicio) {
        this.agora = inicio;
    }

    /** @param quanto tempo a avancar */
    public void avancar(Duration quanto) {
        agora = agora.plus(quanto);
    }

    @Override
    public ZoneId getZone() {
        return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return this;
    }

    @Override
    public Instant instant() {
        return agora;
    }
}
