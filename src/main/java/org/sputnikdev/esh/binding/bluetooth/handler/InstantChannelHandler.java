package org.sputnikdev.esh.binding.bluetooth.handler;

import org.eclipse.smarthome.core.library.types.DateTimeType;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.GregorianCalendar;

/**
 * Date type channel handler.
 *
 * @author Vlad Kolotov
 */
abstract class InstantChannelHandler extends SingleChannelHandler<Instant, DateTimeType> {

    InstantChannelHandler(BluetoothHandler handler, String channelID) {
        super(handler, channelID);
    }

    @Override protected Instant convert(DateTimeType value) {
        if (value == null) {
            return null;
        }
        return value.getCalendar().toInstant();
    }

    @Override protected DateTimeType convert(Instant value) {
        if (value == null) {
            return null;
        }
        Calendar calendar = GregorianCalendar.from(ZonedDateTime.ofInstant(Instant.now(), ZoneId.systemDefault()));
        return new DateTimeType(calendar);
    }

    @Override protected Instant load(Object stored) {
        //TODO figure out how it is stored (what type)
        return null;
    }

}
