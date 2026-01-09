/*
 EcmDroid - Android Diagnostic Tool for Buell Motorcycles
 Copyright (C) 2012 by Michel Marti

 This program is free software; you can redistribute it and/or
 modify it under the terms of the GNU General Public License
 as published by the Free Software Foundation; either version 3
 of the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program; if not, see <http://www.gnu.org/licenses/>.
 */
package org.ecmdroid;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import org.ecmdroid.Constants.DataSource;
import org.ecmdroid.Variable.DataType;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Locale;
import java.util.regex.Matcher;

/**
 * Create a Variable based on definitions in the built-in database.
 */
public class DatabaseVariableProvider extends VariableProvider {

	private DBHelper dbHelper;
	private HashMap<String, Variable> cache = new HashMap<String, Variable>();
	private String current_ecm = null;
	private static final boolean D = false;

	public DatabaseVariableProvider(Context ctx) {
		dbHelper = new DBHelper(ctx);
	}

	@Override
	public Collection<String> getRtVariableNames(String ecm) {
		return getRtVariableNames(ecm, null);
	}

	@Override
	public Collection<String> getScalarRtVariableNames(String ecm) {
		return getRtVariableNames(ecm, DataType.SCALAR);
	}

	@Override
	public Collection<String> getBitfieldRtVariableNames(String ecm) {
		return getRtVariableNames(ecm, DataType.BITFIELD);
	}

	private Collection<String> getRtVariableNames(String ecm, DataType type) {
		LinkedList<String> ret = new LinkedList<String>();
		SQLiteDatabase db = dbHelper.getReadableDatabase();
		try {
			String query = "SELECT names.origname FROM names, rtoffsets, eeprom " +
					" WHERE eeprom.name = '" + ecm + "'" +
					" AND rtoffsets.category = eeprom.category" +
					" AND names.varname = rtoffsets.varname" +
					" AND rtoffsets.secret = 0 " +
					" AND names.secret = 0";
			if (type != null) {
				query += " AND UPPER(rtoffsets.type) = '" + type.toString().toUpperCase(Locale.ENGLISH) + "'";
			}
			query += " ORDER BY UPPER(names.origname)";
			if (D) Log.d(TAG, "Query: " + query);
			Cursor cursor = db.rawQuery(query, null);
			while (cursor.moveToNext()) {
				ret.add(cursor.getString(0));
			}
			cursor.close();
		} finally {
			db.close();
		}
		return ret;
	}

	@Override
	public Variable getRtVariable(String ecm, String name) {
		if (ecm != null && !ecm.equals(current_ecm)) {
			cache.clear();
			current_ecm = ecm;
		}
		String key = "rt#" + name;
		Variable ret = cache.get(key);
		if (cache.containsKey(key)) {
			return ret;
		}
		SQLiteDatabase db = dbHelper.getReadableDatabase();
		try {
			String query = "SELECT rtoffsets.*, names.*, eeprom.type as ecm_type FROM rtoffsets, eeprom, names " +
					" WHERE eeprom.name = '" + ecm + "' AND names.origname = '" + name + "'" +
					" AND rtoffsets.category = eeprom.category" +
					" AND names.varname = rtoffsets.varname" +
					" AND names.secret = 0" +
					" AND rtoffsets.secret = 0";
			if (D) Log.d(TAG, "Query: " + query);
			// TODO: Use selection Args?
			Cursor cursor = db.rawQuery(query, null);
			if (cursor.moveToFirst()) {
				ret = convert(cursor, DataSource.RUNTIME_DATA);
			}
			cursor.close();
		} finally {
			db.close();
		}
		cache.put(key, ret);
		return ret;
	}

	@Override
	public Variable getEEPROMVariable(String ecm, String name) {
		if (ecm == null || name == null) {
			return null;
		}
		if (!ecm.equals(current_ecm)) {
			cache.clear();
			current_ecm = ecm;
		}
		String key = "ee#" + name;
		Variable ret = cache.get(key);
		if (cache.containsKey(key)) {
			return ret;
		}
		SQLiteDatabase db = dbHelper.getReadableDatabase();
		try {
			String query = "SELECT eeoffsets.*, names.*, eeprom.type as ecm_type FROM eeoffsets, eeprom, names " +
					" WHERE eeprom.name = '" + ecm + "' AND names.varname = '" + name + "'" +
					" AND eeoffsets.category = eeprom.category" +
					" AND eeoffsets.varname = names.varname";
			if (D) Log.d(TAG, query);
			Cursor cursor = db.rawQuery(query, null);
			ret = convert(cursor, DataSource.EEPROM);
			cursor.close();
		} finally {
			db.close();
		}
		cache.put(key, ret);
		return ret;
	}

	@Override
	public Variable getNearestEEPROMVariable(String ecm, int offset) {
		if (ecm == null) {
			return null;
		}
		SQLiteDatabase db = dbHelper.getReadableDatabase();
		Variable ret = null;
		try {
			String query = "SELECT eeoffsets.*, names.*, eeprom.type as ecm_type FROM eeoffsets, eeprom, names " +
					" WHERE eeprom.name = '" + ecm + "' AND offset <= " + offset +
					" AND eeoffsets.category = eeprom.category" +
					" AND eeoffsets.varname = names.varname" +
					" ORDER BY offset DESC LIMIT 1";
			if (D) Log.d(TAG, query);
			Cursor cursor = db.rawQuery(query, null);
			ret = convert(cursor, DataSource.EEPROM);
			cursor.close();
		} finally {
			db.close();
		}
		return ret;
	}

	@Override
	public String getName(String varname) {
		Matcher matcher = Constants.BIT_PATTERN.matcher(varname);
		if (matcher.matches()) {
			String name = matcher.group(1);
			int bit = Integer.parseInt(matcher.group(2).split(",")[0]);
			return getName(name, bit);
		}
		String result = null;
		SQLiteDatabase db = dbHelper.getReadableDatabase();
		try {
			String query = "SELECT name FROM names WHERE varname = '" + varname + "' LIMIT 1";
			Cursor c = db.rawQuery(query, null);
			if (c.moveToFirst()) {
				result = c.getString(0);
			}
			c.close();
		} finally {
			db.close();
		}
		return result;
	}

	@Override
	public String getName(String varname, int bit) {

		if (bit < 0 || bit > 7) {
			return null;
		}
		String result = null;
		SQLiteDatabase db = dbHelper.getReadableDatabase();
		try {
			String query = "SELECT bitname" + (bit + 1) + " FROM bits WHERE varname = '" + varname + "' LIMIT 1";
			Cursor c = db.rawQuery(query, null);
			if (c.moveToFirst()) {
				result = c.getString(0);
			}
			c.close();
		} finally {
			db.close();
		}
		return result;
	}

	private Variable convert(Cursor cursor, DataSource runtimeData) {
		Variable ret = null;
		if (cursor.moveToFirst()) {
			ret = new Variable();
			int uniqueidIdx = cursor.getColumnIndex("uniqueid");
			if (uniqueidIdx >= 0) {
				ret.setId(cursor.getInt(uniqueidIdx));
			}
			int ecmTypeIdx = cursor.getColumnIndex("ecm_type");
			if (ecmTypeIdx >= 0) {
				ret.setEcmType(ECM.Type.getType(cursor.getString(ecmTypeIdx)));
			}
			int orignameIdx = cursor.getColumnIndex("origname");
			if (orignameIdx >= 0) {
				ret.setName(cursor.getString(orignameIdx));
			}
			if (ret.getName() == null) {
				int varnameIdx = cursor.getColumnIndex("varname");
				if (varnameIdx >= 0) {
					ret.setName(cursor.getString(varnameIdx));
				}
			}
			int typeIdx = cursor.getColumnIndex("type");
			if (typeIdx >= 0) {
				String type = cursor.getString(typeIdx).toUpperCase(Locale.ENGLISH);
				ret.setType(DataType.valueOf(type));
			}
			int sizeIdx = cursor.getColumnIndex("size");
			if (sizeIdx >= 0) {
				ret.setSize(cursor.getInt(sizeIdx));
			}
			if (DataSource.EEPROM.equals(runtimeData)) {
				int elemsizeIdx = cursor.getColumnIndex("elemsize");
				if (elemsizeIdx >= 0) {
					ret.setWidth(cursor.getInt(elemsizeIdx));
				}
				int colsIdx = cursor.getColumnIndex("cols");
				if (colsIdx >= 0) {
					ret.setCols(cursor.getInt(colsIdx));
				}
				int rowsIdx = cursor.getColumnIndex("rows");
				if (rowsIdx >= 0) {
					ret.setRows(cursor.getInt(rowsIdx));
				}
			} else {
				ret.setWidth(ret.getSize());
			}
			int offsetIdx = cursor.getColumnIndex("offset");
			if (offsetIdx >= 0) {
				ret.setOffset(cursor.getInt(offsetIdx));
			}
			int scaleIdx = cursor.getColumnIndex("scale");
			if (scaleIdx >= 0) {
				ret.setScale(cursor.getDouble(scaleIdx));
			}
			int translateIdx = cursor.getColumnIndex("translate");
			if (translateIdx >= 0) {
				ret.setTranslate(cursor.getDouble(translateIdx));
			}
			int formatIdx = cursor.getColumnIndex("format");
			if (formatIdx >= 0) {
				ret.setFormat(cursor.getString(formatIdx));
			}
			int nameIdx = cursor.getColumnIndex("name");
			if (nameIdx >= 0) {
				ret.setLabel(cursor.getString(nameIdx));
			}
			int remarkIdx = cursor.getColumnIndex("remark");
			if (remarkIdx >= 0) {
				ret.setRemarks(cursor.getString(remarkIdx));
			}
			int descriptionIdx = cursor.getColumnIndex("description");
			if (descriptionIdx >= 0) {
				ret.setDescription(cursor.getString(descriptionIdx));
			}
			int unitsIdx = cursor.getColumnIndex("units");
			if (unitsIdx >= 0) {
				ret.setUnit(cursor.getString(unitsIdx));
			}
			ret.setSymbol(Units.getSymbol(ret.getUnit()));

			if (DataSource.RUNTIME_DATA.equals(runtimeData)) {
				int lowIdx = cursor.getColumnIndex("low");
				if (lowIdx >= 0) {
					ret.setLow(cursor.getDouble(lowIdx));
				}
				int highIdx = cursor.getColumnIndex("high");
				if (highIdx >= 0) {
					ret.setHigh(cursor.getDouble(highIdx));
				}
				int ulowIdx = cursor.getColumnIndex("ulow");
				if (ulowIdx >= 0) {
					ret.setUlow(cursor.getInt(ulowIdx));
				}
				int uhighIdx = cursor.getColumnIndex("uhigh");
				if (uhighIdx >= 0) {
					ret.setUhigh(cursor.getInt(uhighIdx));
				}
			}
			ret.init();
		}
		return ret;
	}
}
