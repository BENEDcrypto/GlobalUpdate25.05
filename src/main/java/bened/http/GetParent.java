package bened.http;

import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;
import bened.Account;
import bened.Bened;
import bened.BNDException;
import bened.util.Convert;
import bened.util.BenedTree;

import javax.servlet.http.HttpServletRequest;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GetParent extends BenedTree.APIHierarchyRequestHandler {

    static final GetParent instance = new GetParent();
    private GetParent () {
       
        super (new APITag[] {APITag.ACCOUNTS}, "account");      
    }
    @Override
    protected JSONStreamAware processHierarchyRequest(HttpServletRequest request) throws BNDException {

        long account = ParameterParser.getAccountId(request, true);

        if (account == 0L || Account.getAccount(account) == null )
            return BenedTree.createErrorResponse("Invalid \"account\"!", 9999);

        JSONObject response = new JSONObject();
        
        try(PreparedStatement statement = Bened.softMG().getConnection().prepareStatement("select parent_id from soft where id=?");){
            statement.setLong(1, account);
            try(ResultSet rs = statement.executeQuery();){
            long solvedParent = 0l;
            while (rs.next()) {
                solvedParent = rs.getLong(1);
            }
            String rsParent = Convert.rsAccount(solvedParent);

            response.put("accountRS", Convert.rsAccount(account));
            response.put("parentRS", (solvedParent==0?"":rsParent) );
            response.put("parent", (solvedParent==0?"":Convert.parseAccountId(rsParent)) );
            
            }//rs.close();
        } catch (SQLException ex) {
            Logger.getLogger(GetParent.class.getName()).log(Level.SEVERE, null, ex);
        }//statement.close();
            
            return response;
        
    }
}
