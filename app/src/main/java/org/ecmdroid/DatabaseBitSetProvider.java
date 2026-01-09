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

import org.ecmdroid.Constants.DataSource;

import java.util.HashMap;

/**
 * Create a BitSet based on definitions in the built-in database.
 */
public class DatabaseBitSetProvider extends BitSetProvider {

	private DBHelper dbHelper;
	private HashMap<String, BitSet> cache;
	private String current_ecm = null;

	public DatabaseBitSetProvider(Context ctx) {
		dbHelper = new DBHelper(ctx);
		cache = new HashMap<String, BitSet>();
	}

	@Override
	public BitSet getBitSet(String ecm_id, String name, DataSource source) {
		if (ecm_id != null && !ecm_id.equals(current_ecm)) {
			cache.clear();
			current_ecm = ecm_id;
		}
		BitSet ret = cache.get(name);
		if (cache.containsKey(name)) {
			return ret;
		}
		SQLiteDatabase db = dbHelper.getReadableDatabase();
		String offset_table = (source == DataSource.EEPROM ? "eeoffsets" : "rtoffsets");
		try {
			String query = "SELECT * FROM " + offset_table + " AS offsets, bits, eeprom" +
					" WHERE offsets.varname = '" + name + "'" +
					" AND bits.varname = offsets.varname" +
					" AND eeprom.name = '" + ecm_id + "'" +
					" AND offsets.category = eeprom.category";
			if (source == DataSource.RUNTIME_DATA) {
				query += " AND offsets.secret = 0";
			}
			// Log.d(TAG, "Query: " + query);
			Cursor c = db.rawQuery(query, null);
			if (c.moveToFirst()) {
				int varnameIdx = c.getColumnIndex("varname");
				int nameIdx = c.getColumnIndex("name");
				int offsetIdx = c.getColumnIndex("offset");
				if (varnameIdx < 0 || nameIdx < 0 || offsetIdx < 0) {
					c.close();
					return null;
				}
				String setname = c.getString(varnameIdx);
				String label = c.getString(nameIdx);
				int offset = c.getInt(offsetIdx);
				ret = new BitSet(setname, label, offset);
				for (int i = 1; i <= 8; i++) {
					int bitnameIdx = c.getColumnIndex("bitname" + i);
					int bitIdx = c.getColumnIndex("bit" + i);
					if (bitnameIdx < 0 || bitIdx < 0) {
						continue;
					}
					String bitname = c.getString(bitnameIdx);
					String bitdesc = c.getString(bitIdx);
					if (Utils.isEmptyString(bitname) && Utils.isEmptyString(bitdesc)) {
						continue;
					}
					if (Utils.isEmptyString(bitname)) {
						bitname = setname + "." + i;
					}
					Bit bit = new Bit();
					bit.setName(bitname);
					bit.setBitNr(i - 1);
					int byteIdx = c.getColumnIndex("byte");
					if (byteIdx >= 0) {
						bit.setByteNr(c.getInt(byteIdx));
					}
					bit.setOffset(offset);
					int typeIdx = c.getColumnIndex("type");
					if (typeIdx >= 0) {
						bit.setType(ECM.Type.getType(c.getString(typeIdx)));
					}
					bit.setRemark(bitdesc);
					int dtcIdx = c.getColumnIndex("dtc" + i);
					if (dtcIdx >= 0) {
						bit.setCode(c.getString(dtcIdx));
					}
					// Log.d(TAG, bit.toString());
					ret.add(bit);
				}
			}
			c.close();
		} finally {
			db.close();
		}
		cache.put(name, ret);
		return ret;
	}

}
