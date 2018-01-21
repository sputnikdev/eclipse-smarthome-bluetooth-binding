package org.sputnikdev.esh.binding.bluetooth.handler;

import org.eclipse.smarthome.core.library.types.DateTimeType;

import java.util.Calendar;
import java.util.Date;

/**
 * Date type channel handler.
 *
 * @author Vlad Kolotov
 */
abstract class DateTimeChannelHandler extends SingleChannelHandler<Date, DateTimeType> {

    DateTimeChannelHandler(BluetoothHandler handler, String channelID) {
        super(handler, channelID);
    }

    @Override protected  Date convert(DateTimeType value) {
        if (value == null) {
            return null;
        }
        return value.getCalendar().getTime();
    }

    @Override protected DateTimeType convert(Date value) {
        if (value == null) {
            return null;
        }
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(value);
        return new DateTimeType(calendar);
    }

    @Override protected  Date load(Object stored) {
        //TODO figure out how it is stored (what type)
        return null;
    }

}
