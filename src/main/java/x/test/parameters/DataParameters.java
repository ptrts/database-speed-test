package x.test.parameters;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class DataParameters {

    @Id
    public int id = 1;

    public int campaignsNumber;
    public int usersPerCampaign;
    public int usersNumber;

    public int getMessageTableSize() {
        return campaignsNumber * usersPerCampaign;
    }
}
