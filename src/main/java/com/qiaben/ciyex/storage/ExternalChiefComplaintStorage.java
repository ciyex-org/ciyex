//package com.qiaben.ciyex.storage;
//
//import com.qiaben.ciyex.dto.ChiefComplaintDto;
//
//public interface ExternalChiefComplaintStorage {
//    void saveChiefComplaint(ChiefComplaintDto chiefComplaintDto);
//    ChiefComplaintDto getChiefComplaintById(Long id);
//}


package com.qiaben.ciyex.storage;

import com.qiaben.ciyex.dto.ChiefComplaintDto;

public interface ExternalChiefComplaintStorage {
    void saveChiefComplaint(ChiefComplaintDto chiefComplaintDto);
    ChiefComplaintDto getChiefComplaintById(Long id);
}
