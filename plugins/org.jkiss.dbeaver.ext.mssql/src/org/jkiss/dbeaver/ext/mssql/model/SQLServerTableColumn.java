/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.ext.mssql.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCTableColumn;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.DBSDataType;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;
import org.jkiss.dbeaver.model.struct.DBSTypedObjectEx;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableColumn;
import org.jkiss.utils.CommonUtils;

import java.sql.ResultSet;

/**
 * SQLServerTableColumn
 */
public class SQLServerTableColumn extends JDBCTableColumn<SQLServerTableBase> implements DBSTableColumn, DBSTypedObjectEx, DBPNamedObject2, DBPOrderedObject, DBPHiddenObject, SQLServerObject {
    private static final Log log = Log.getLog(SQLServerTableColumn.class);

    private String comment;
    private long objectId;
    private int userTypeId;
    private SQLServerDataType dataType;
    private String collationName;
    private boolean hidden;

    public SQLServerTableColumn(SQLServerTableBase table) {
        super(table, false);
    }

    public SQLServerTableColumn(
        DBRProgressMonitor monitor,
        SQLServerTableBase table,
        ResultSet dbResult)
        throws DBException {
        super(table, true);
        loadInfo(monitor, dbResult);
    }

    // Copy constructor
    public SQLServerTableColumn(
        DBRProgressMonitor monitor,
        SQLServerTableBase table,
        DBSEntityAttribute source)
        throws DBException {
        super(table, source, false);
        this.comment = source.getDescription();
        if (source instanceof SQLServerTableColumn) {
            SQLServerTableColumn mySource = (SQLServerTableColumn) source;
            // Copy
        }
    }

    private void loadInfo(DBRProgressMonitor monitor, ResultSet dbResult)
        throws DBException {
        this.objectId = JDBCUtils.safeGetLong(dbResult, "column_id");

        setName(JDBCUtils.safeGetString(dbResult, "name"));
        setOrdinalPosition(JDBCUtils.safeGetInt(dbResult, "column_id"));

        this.userTypeId = JDBCUtils.safeGetInt(dbResult, "user_type_id");
        this.dataType = getTable().getDatabase().getDataType(monitor, userTypeId);

        setMaxLength(JDBCUtils.safeGetLong(dbResult, "max_length"));
        setRequired(JDBCUtils.safeGetInt(dbResult, "is_nullable") != 0);
        setScale(JDBCUtils.safeGetInteger(dbResult, "scale"));
        setPrecision(JDBCUtils.safeGetInteger(dbResult, "precision"));
        setAutoGenerated(JDBCUtils.safeGetInt(dbResult, "is_identity") != 0);

        this.hidden = JDBCUtils.safeGetInt(dbResult, "is_hidden") != 0;
        this.collationName = JDBCUtils.safeGetString(dbResult, "collation_name");
        String dv = JDBCUtils.safeGetString(dbResult, "default_definition");
        if (!CommonUtils.isEmpty(dv)) {
            // Remove redundant brackets
            while (dv.startsWith("(") && dv.endsWith(")")) {
                dv = dv.substring(1, dv.length() - 1);
            }
            this.setDefaultValue(dv);
        }
    }

    @NotNull
    @Override
    public SQLServerDataSource getDataSource() {
        return getTable().getDataSource();
    }

    @Property(viewable = true, editable = true, updatable = true, order = 20, listProvider = ColumnTypeNameListProvider.class)
    public String getFullTypeName() {
        if (dataType == null) {
            return String.valueOf(userTypeId);
        }

        String typeName = dataType.getName();
        String typeModifiers = SQLUtils.getColumnTypeModifiers(getDataSource(), this, typeName, dataType.getDataKind());
        return typeModifiers == null ? typeName : (typeName + CommonUtils.notEmpty(typeModifiers));
    }

    @Override
    public String getTypeName() {
        return dataType == null ? String.valueOf(userTypeId) : dataType.getTypeName();
    }

    @Override
    public DBSDataType getDataType() {
        return dataType;
    }

    public void setDataType(SQLServerDataType dataType) {
        this.dataType = dataType;
    }

    @Override
    public DBPDataKind getDataKind() {
        return dataType == null ? DBPDataKind.UNKNOWN : dataType.getDataKind();
    }

/*
    //@Property(viewable = true, editable = true, updatable = true, order = 40)
    @Override
    public long getMaxLength()
    {
        return super.getMaxLength();
    }

    @Override
    //@Property(viewable = true, order = 41)
    public Integer getScale()
    {
        return super.getScale();
    }

    @Override
    //@Property(viewable = true, order = 42)
    public Integer getPrecision()
    {
        return super.getPrecision();
    }

    @Property(viewable = true, editable = true, updatable = true, order = 50)
    @Override
    public boolean isRequired()
    {
        return super.isRequired();
    }

    @Override
    @Property(viewable = true, editable = true, updatable = true, order = 51)
    public boolean isAutoGenerated()
    {
        return autoGenerated;
    }

    @Override
    @Property(viewable = true, editable = true, updatable = true, order = 70)
    public String getDefaultValue()
    {
        return super.getDefaultValue();
    }
*/

    @Property(viewable = false, order = 75)
    public String getCollationName() {
        return collationName;
    }

    @Property(viewable = false, order = 80)
    @Override
    public boolean isHidden() {
        return hidden;
    }

    @Property(viewable = true, editable = true, updatable = true, multiline = true, order = 100)
    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    @Nullable
    @Override
    public String getDescription() {
        return getComment();
    }

    @Override
    public long getObjectId() {
        return objectId;
    }

}