package com.constanttherapy.users;

import com.constanttherapy.db.SQLUtil;
import com.constanttherapy.db.SqlPreparedStatement;
import com.constanttherapy.users.CTUser;
import com.constanttherapy.users.Patient;
import com.constanttherapy.util.CTTestBase;
import com.constanttherapy.util.StringUtil;
import org.junit.Test;

import java.sql.SQLException;
import java.util.List;

public class PatientTest extends CTTestBase
{

	@Test
	public void test_generateDummyData() throws Exception
	{
		Patient p = (Patient) CTUser.getById(_sql, 10);
		p.generateDummyData(_sql);

		//TODO: teardown the dummy data
	}

	@Test
	public void test_getDeficits() throws Exception
	{
		Patient p = (Patient) CTUser.getById(_sql, 9999);
		List<String> list = p.getDeficits(_sqlr);

		System.out.println(StringUtil.listToCSV(list, true));
		System.out.println(StringUtil.listToCSV(list, false));
	}

    @Deprecated
    private void deletePatientStats(int patientId) throws SQLException
    {
        SqlPreparedStatement statement = null;

        try
        {
            //sql = SQLUtil.getDatabaseConnection();
            String q = "delete from patient_stats where patient_id = ?";
            statement = _sql.prepareStatement(q);
            statement.setInt(1, patientId);

            statement.execute();
        }
        finally
        {
            SQLUtil.closeQuietly(statement);
        }
    }
}
