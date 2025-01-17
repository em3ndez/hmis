/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.bean.channel;

import com.divudi.bean.common.SessionController;

import com.divudi.data.FeeChangeType;
import com.divudi.data.FeeType;
import com.divudi.data.PersonInstitutionType;
import com.divudi.entity.BillSession;
import com.divudi.entity.Department;
import com.divudi.entity.FeeChange;
import com.divudi.entity.ItemFee;
import com.divudi.entity.ServiceSession;
import com.divudi.entity.SessionNumberGenerator;
import com.divudi.entity.Speciality;
import com.divudi.entity.Staff;
import com.divudi.facade.DepartmentFacade;
import com.divudi.facade.FeeChangeFacade;
import com.divudi.facade.FeeFacade;
import com.divudi.facade.ItemFeeFacade;
import com.divudi.facade.ServiceSessionFacade;
import com.divudi.facade.SessionNumberGeneratorFacade;
import com.divudi.facade.StaffFacade;
import com.divudi.bean.common.util.JsfUtil;
import com.divudi.java.CommonFunctions;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.TemporalType;

/**
 *
 * @author safrin
 */
@Named
@SessionScoped
public class SheduleController implements Serializable {

    @EJB
    private StaffFacade staffFacade;
    @EJB
    private ServiceSessionFacade facade;
    @EJB
    private FeeFacade feeFacade;
    @EJB
    FeeChangeFacade feeChangeFacade;
    @EJB
    SessionNumberGeneratorFacade sessionNumberGeneratorFacade;
    @EJB
    ServiceSessionFacade serviceSessionFacade;
    @Inject
    private SessionController sessionController;
    private Speciality speciality;
    ServiceSession current;
    private Staff currentStaff;
    private List<ServiceSession> filteredValue;
    List<SessionNumberGenerator> lstSessionNumberGenerator;
    List<ItemFee> itemFees;
    //change Fee
    Date effectiveDate;
    List<FeeChange> feeChanges;
    List<FeeChange> feeChangesList;
    boolean feeChangeStaff;

    public List<ItemFee> getItemFees() {
        if (itemFees == null) {
            itemFees = new ArrayList<>();
        }
        return itemFees;
    }

    public void setItemFees(List<ItemFee> itemFees) {
        this.itemFees = itemFees;
    }

    @EJB
    ItemFeeFacade itemFeeFacade;

    public void fillFees() {
        String sql;
        Map m = new HashMap();
        sql = "Select f from ItemFee f "
                + " where f.retired=false "
                + " and (f.serviceSession=:ses "
                + " or f.item=:ses )"
                + " order by f.id";
        m.put("ses", current);
        itemFees = itemFeeFacade.findByJpql(sql, m);
    }

    public ItemFee createStaffFee() {
        ItemFee stf = new ItemFee();
        stf.setName("Doctor Fee");
        stf.setFeeType(FeeType.Staff);
        stf.setFee(0.0);
        stf.setFfee(0.0);
        stf.setInstitution(getCurrent().getInstitution());
        stf.setSpeciality(speciality);
        stf.setStaff(currentStaff);
        stf.setServiceSession(current);
        return stf;
    }

    public ItemFee createHospitalFee() {
        ItemFee hos = new ItemFee();
        hos.setName("Hospital Fee");
        hos.setFeeType(FeeType.OwnInstitution);
        hos.setFee(0.0);
        hos.setFfee(0.0);
        hos.setInstitution(getCurrent().getInstitution());
        if (getCurrent().getDepartment() != null) {
            hos.setDepartment(getCurrent().getDepartment());
        }else{
            hos.setDepartment(getSessionController().getDepartment());
        }
        hos.setServiceSession(current);

        return hos;
    }

    public ItemFee createAgencyFee() {
        ItemFee agency = new ItemFee();
        agency.setName("Agency Fee");
        agency.setFeeType(FeeType.OtherInstitution);
        agency.setFee(0.0);
        agency.setFfee(0.0);
        agency.setServiceSession(current);
        return agency;
    }

    public ItemFee createScanFee() {
        ItemFee scn = new ItemFee();
        scn.setName("Scan Fee");
        scn.setFee(0.0);
        scn.setFfee(0.0);
        scn.setFeeType(FeeType.Service);
        scn.setInstitution(getCurrent().getInstitution());
        scn.setServiceSession(current);
        return scn;
    }

    public ItemFee createOnCallFee() {
        ItemFee onc = new ItemFee();
        onc.setName("On-Call Fee");
        onc.setFeeType(FeeType.OwnInstitution);
        onc.setFee(0.0);
        onc.setFfee(0.0);
        onc.setInstitution(getCurrent().getInstitution());
        onc.setServiceSession(current);
        return onc;
    }

    private void createFees() {
        getItemFees().add(createStaffFee());
        getItemFees().add(createHospitalFee());
        getItemFees().add(createAgencyFee());
        getItemFees().add(createScanFee());
        getItemFees().add(createOnCallFee());
    }

    public void makeNull() {
        speciality = null;
        current = null;
        currentStaff = null;
        filteredValue = null;
        itemFees = null;
    }

    public List<Staff> completeStaff(String query) {
        List<Staff> suggestions;
        String sql;
        Map m = new HashMap();
        m.put("name", "%" + query.toUpperCase() + "%");
        m.put("code", "%" + query.toUpperCase() + "%");
        if (getSpeciality() != null) {
            sql = "select p from Staff p where p.retired=false and ((p.person.name) like :name or  (p.code) like :code ) and p.speciality =:sp order by p.person.name";
            m.put("sp", speciality);
        } else {
            sql = "select p from Staff p where p.retired=false and ((p.person.name) like :name or  (p.code) like :code ) order by p.person.name";
        }
        suggestions = getStaffFacade().findByJpql(sql, m);
        return suggestions;
    }

    public List<Staff> getConsultants() {
        List<Staff> suggestions;
        String sql;

        if (getSpeciality() != null) {
            sql = "select p from Staff p where p.retired=false and p.speciality.id = " + getSpeciality().getId() + " order by p.person.name";
        } else {
            sql = "select p from Staff p where p.retired=false order by p.person.name";
        }
        ////System.out.println(sql);
        suggestions = getStaffFacade().findByJpql(sql);

        return suggestions;
    }

    public List<ServiceSession> completeSession(String query) {
        List<ServiceSession> suggestions;
        String sql;
        if (query == null) {
            suggestions = new ArrayList<>();
        } else {
            if (getCurrentStaff() != null) {
                sql = "select p from ServiceSession p where p.retired=false and (p.name) like '%" + query.toUpperCase() + "%' and p.staff.id = " + getCurrentStaff().getId() + " order by p.name";
                suggestions = getFacade().findByJpql(sql);
            } else {
                suggestions = new ArrayList<>();
            }

        }
        return suggestions;
    }

    public List<SessionNumberGenerator> getLstSessionNumberGenerator() {
        return lstSessionNumberGenerator;
    }

    public void setLstSessionNumberGenerator(List<SessionNumberGenerator> lstSessionNumberGenerator) {
        this.lstSessionNumberGenerator = lstSessionNumberGenerator;
    }

    public SheduleController() {
    }

    public Speciality getSpeciality() {
        return speciality;
    }

    public void setSpeciality(Speciality speciality) {
        currentStaff = null;
        this.speciality = speciality;

    }

    public StaffFacade getStaffFacade() {
        return staffFacade;
    }

    public void setStaffFacade(StaffFacade staffFacade) {
        this.staffFacade = staffFacade;
    }

    @EJB
    DepartmentFacade departmentFacade;

    public List<Department> getInstitutionDepatrments() {
        List<Department> d;
        if (getCurrent().getInstitution() == null) {
            return new ArrayList<>();
        } else {
            String sql = "Select d From Department d where d.retired=false and d.institution.id=" + getCurrent().getInstitution().getId();
            d = departmentFacade.findByJpql(sql);
        }

        return d;
    }

    public ServiceSession getCurrent() {
        if (current == null) {
            current = new ServiceSession();
            current.setInstitution(sessionController.getInstitution());
            current.setDepartment(sessionController.getDepartment());
//            createFees();
        }
        return current;
    }

    public SessionNumberGeneratorFacade getSessionNumberGeneratorFacade() {
        return sessionNumberGeneratorFacade;
    }

    public void setSessionNumberGeneratorFacade(SessionNumberGeneratorFacade sessionNumberGeneratorFacade) {
        this.sessionNumberGeneratorFacade = sessionNumberGeneratorFacade;
    }

    public void setCurrent(ServiceSession current) {
        this.current = current;
    }

    public List<ServiceSession> getItems() {
        List<ServiceSession> items;
        String sql;
        HashMap hm = new HashMap();
        sql = "Select s From ServiceSession s "
                + " where s.retired=false "
                + " and type(s)=:class "
                + " and s.staff=:stf "
                + " and s.originatingSession is null "
                + " order by s.sessionWeekday,s.startingTime ";
        hm.put("stf", currentStaff);
        hm.put("class", ServiceSession.class);
        items = getFacade().findByJpql(sql, hm);

        return items;
    }

    public List<ItemFee> fetchStaffServiceSessions() {
        String sql;
        HashMap m = new HashMap();
        sql = "Select i From ItemFee i join i.serviceSession s "
                + " where s.retired=false "
                + " and type(s)=:class "
                + " and s.originatingSession is null ";
        if (currentStaff != null) {
            sql += " and s.staff=:stf ";
            m.put("stf", currentStaff);
        }
        sql += " order by s.staff.person.name,s.sessionWeekday,s.startingTime ";

        m.put("class", ServiceSession.class);

        return itemFeeFacade.findByJpql(sql, m);
    }

    public void prepareAdd() {
        current = null;
        itemFees = null;
        createFees();
    }

    public void prepareAddFeeChange() {
        prepareAdd();
        createChangeFees();
    }

    public ServiceSessionFacade getFacade() {
        return facade;
    }

    public void setFacade(ServiceSessionFacade facade) {
        this.facade = facade;
    }

    public void delete() {

        if (checkError(current)) {
            JsfUtil.addErrorMessage("you can't Remove This Shedule.Because This Shedule has Channeling Bills");
            return;
        }

        if (current != null) {
            current.setRetired(true);
            current.setRetiredAt(new Date());
            current.setRetirer(getSessionController().getLoggedUser());
            getFacade().edit(current);
            if (checkSessionNumberGenerater(current)) {
                current.getSessionNumberGenerator().setRetired(true);
                current.getSessionNumberGenerator().setRetiredAt(new Date());
                current.getSessionNumberGenerator().setRetirer(getSessionController().getLoggedUser());
                sessionNumberGeneratorFacade.edit(current.getSessionNumberGenerator());
            }
            JsfUtil.addSuccessMessage("Deleted Successfully");
        } else {
            JsfUtil.addSuccessMessage("Nothing to Delete");
        }

        getItems();
        current = null;
        getCurrent();
    }

    private boolean checkError() {
        if (current.getStartingTime() == null) {
            JsfUtil.addErrorMessage("Starting time Must be Filled");
            return true;
        }
        if (current.getName() == null || current.getName().trim().equals("")) {
            JsfUtil.addErrorMessage("Please Select Session Name");
            return true;
        }

        if (current.getSessionWeekday() == null && getCurrent().getSessionDate() == null) {
            JsfUtil.addErrorMessage("Set Weekday or Date");
            return true;
        }

        if (speciality == null) {
            JsfUtil.addErrorMessage("Plaese Select Specility");
            return true;
        }

        if (currentStaff == null) {
            JsfUtil.addErrorMessage("Plaese Select Doctor");
            return true;
        }

        return false;
    }

    private boolean checkError(ServiceSession ss) {
        String sql;
        Map m = new HashMap();
        sql = " select bs.serviceSession from BillSession bs where "
                + " bs.retired=false "
                + " and bs.sessionDate>:td "
                + " and bs.serviceSession.originatingSession=:ss ";
        m.put("ss", ss);
        m.put("td", new Date());
        List<ServiceSession> sss = getFacade().findByJpql(sql, m, TemporalType.TIMESTAMP);
//        double d=getFacade().findAggregateLong(sql, m, TemporalType.TIMESTAMP);
        return sss.size() > 0;
    }

    public boolean checkSessionNumberGenerater(ServiceSession ss) {
        String sql;
        Map m = new HashMap();
        sql = "Select s From ServiceSession s "
                + " where s.retired=false "
                + " and type(s)=:class "
                + " and s.sessionNumberGenerator=:sg"
                + " and s!=:ss "
                + " and s.originatingSession is null "
                + " order by s.sessionWeekday,s.startingTime ";
        m.put("sg", ss.getSessionNumberGenerator());
        m.put("ss", ss);
        m.put("class", ServiceSession.class);
        List<ServiceSession> sss = getFacade().findByJpql(sql, m, TemporalType.TIMESTAMP);
        sss = getFacade().findByJpql(sql, m, TemporalType.TIMESTAMP);
        return sss.isEmpty();
    }

    public SessionNumberGenerator saveSessionNumber() {
        SessionNumberGenerator sessionNumberGenerator = new SessionNumberGenerator();
        sessionNumberGenerator.setSpeciality(speciality);
        sessionNumberGenerator.setStaff(currentStaff);
        sessionNumberGenerator.setName(currentStaff.getPerson().getName() + " " + current.getName());
        sessionNumberGeneratorFacade.create(sessionNumberGenerator);
        return sessionNumberGenerator;
    }

    public void resetSessionNumbers() {

        String sql;
        sql = " SELECT sg FROM ServiceSession sg WHERE sg.retired=false";
        List<ServiceSession> list = facade.findByJpql(sql);

        for (ServiceSession sng : list) {
            if (sng.getSessionNumberGenerator() != null) {
                continue;
            }
            SessionNumberGenerator sessionNumberGenerator = new SessionNumberGenerator();
            sessionNumberGenerator.setSpeciality(sng.getStaff().getSpeciality());
            sessionNumberGenerator.setStaff(sng.getStaff());
            sessionNumberGenerator.setName(sng.getStaff().getPerson().getName() + " " + sng.getName());
            sessionNumberGeneratorFacade.create(sessionNumberGenerator);

            sng.setSessionNumberGenerator(sessionNumberGenerator);
            facade.edit(sng);
        }

    }

    private void saveFees(ServiceSession serviceSession) {
        if (getItemFees() == null) {
            return;
        }

        for (ItemFee i : getItemFees()) {
            i.setServiceSession(serviceSession);
            i.setItem(serviceSession);
            i.setInstitution(serviceSession.getInstitution());
            if (i.getId() == null) {
                i.setCreatedAt(new Date());
                i.setCreater(sessionController.getLoggedUser());
                itemFeeFacade.create(i);
            } else {
                itemFeeFacade.edit(i);
            }

        }
    }

    public void saveSelected() {
        //System.out.println("session name"+current.getName());
        if (checkError()) {
            return;
        }

        if (getCurrent().getSessionNumberGenerator() == null) {
            SessionNumberGenerator ss = saveSessionNumber();
            current.setSessionNumberGenerator(ss);
        }


        getCurrent().setStaff(currentStaff);
        if (getCurrent().getId() != null && getCurrent().getId() > 0) {
            getFacade().edit(getCurrent());
            JsfUtil.addSuccessMessage("Updated Successfully.");
        } else {
            getCurrent().setCreatedAt(new Date());
            getCurrent().setCreater(getSessionController().getLoggedUser());
            getFacade().create(getCurrent());
            JsfUtil.addSuccessMessage("Saved Successfully");
        }

        saveFees(getCurrent());

        getCurrent().setTotal(calTot());
        getCurrent().setTotalFfee(calFTot());

        facade.edit(getCurrent());
        updateCreatedServicesesions(getCurrent());
        prepareAdd();
        getItems();
    }

    public void updateCreatedServicesesions(ServiceSession ss) {
        for (ServiceSession i : fetchCreatedServiceSessions(ss)) {
            System.out.println("i.getName() = " + i.getName());

            i.setName(ss.getName());
            i.setInstitution(ss.getInstitution());
            i.setDepartment(ss.getDepartment());
            i.setStartingTime(ss.getStartingTime());
            i.setEndingTime(ss.getEndingTime());
            i.setMaxNo(ss.getMaxNo());
            i.setDuration(ss.getDuration());
            i.setRoomNo(ss.getRoomNo());
            i.setAfterSession(ss.getAfterSession());
            i.setBeforeSession(ss.getBeforeSession());
            i.setDisplayCount(ss.getDisplayCount());
            i.setDisplayPercent(ss.getDisplayPercent());
            i.setRefundable(ss.isRefundable());
            i.setCreditNumbers(ss.getCreditNumbers());
            i.setCashNumbers(ss.getCashNumbers());
            i.setAgencyNumbers(ss.getAgencyNumbers());
            i.setReserveNumbers(ss.getReserveNumbers());
            i.setReserveName(ss.getReserveName());
            i.setMaxTableRows(ss.getMaxTableRows());
            i.setSessionWeekday(ss.getSessionWeekday());

            getFacade().edit(i);
        }
    }

    public List<ServiceSession> fetchCreatedServiceSessions(ServiceSession ss) {
        List<ServiceSession> items;
        String sql;
        HashMap m = new HashMap();
        sql = "Select s From ServiceSession s "
                + " where s.retired=false "
                + " and type(s)=:class "
                + " and s.originatingSession=:ss"
                + " and s.sessionDate>=:sd "
                + " order by s.sessionWeekday,s.startingTime ";
        m.put("ss", ss);
        m.put("class", ServiceSession.class);
        m.put("sd", CommonFunctions.getStartOfDay());
        items = getFacade().findByJpql(sql, m);
        return items;
    }

    public void createOnCallFeeOldSession() {
        String sql;
        Map m = new HashMap();
        sql = "Select DISTINCT(f.serviceSession) from ItemFee f "
                + " where f.retired=false "
                + " and f.serviceSession is not null ";
        List<ServiceSession> serviceSessionsAll = serviceSessionFacade.findByJpql(sql);
        sql = "Select DISTINCT(f.serviceSession) from ItemFee f "
                + " where f.retired=false "
                + " and f.serviceSession is not null "
                + " and f.feeType=:fType "
                + " and f.name=:name "
                + " order by f.id";
        m.put("name", "On-Call Fee");
        m.put("fType", FeeType.OwnInstitution);
        List<ServiceSession> serviceSessions = serviceSessionFacade.findByJpql(sql, m);
        serviceSessionsAll.removeAll(serviceSessions);
        for (ServiceSession ss : serviceSessionsAll) {
            ItemFee onc = new ItemFee();
            onc.setName("On-Call Fee");
            onc.setFeeType(FeeType.OwnInstitution);
            onc.setFee(0.0);
            onc.setFfee(0.0);
            onc.setInstitution(ss.getInstitution());
            onc.setServiceSession(ss);
            onc.setItem(ss);
            itemFeeFacade.create(onc);
        }

    }

    private double calTot() {
        double tot = 0.0;
        for (ItemFee i : getItemFees()) {
            tot += i.getFee();
        }
        return tot;
    }

    private double calFTot() {
        double tot = 0.0;
        for (ItemFee i : getItemFees()) {
            tot += i.getFfee();
        }
        return tot;
    }

    public void createChangeFees() {
        feeChanges = new ArrayList<>();
        for (ItemFee f : itemFees) {
            FeeChange fc = new FeeChange();
            fc.setFee(f);
            fc.getFee().setStaff(null);
            fc.getFee().setSpeciality(null);
            fc.getFee().setServiceSession(null);
            fc.setFeeChangeType(FeeChangeType.Channel);
            fc.setDone(false);
            feeChanges.add(fc);
        }
    }

    public void saveFeeChanges() {
        Date nowDate = CommonFunctions.getEndOfDay(new Date());
        if (nowDate.before(effectiveDate)) {
            JsfUtil.addErrorMessage("Please Select Future Date");
            return;
        }
        if (feeChangeStaff) {
            if (speciality == null) {
                JsfUtil.addErrorMessage("Please Select Specility");
                return;
            }
            if (currentStaff == null) {
                JsfUtil.addErrorMessage("Please Select Staff");
                return;
            }
            for (FeeChange fc : feeChanges) {
                fc.getFee().setStaff(currentStaff);
                fc.getFee().setSpeciality(speciality);
            }
        }
        String sql;
        Map m = new HashMap();
        sql = " select fc from FeeChange fc where "
                + " fc.retired=false "
                + " and fc.validFrom=:ed ";
        if (feeChangeStaff) {
            sql += " and fc.fee.staff=:s "
                    + " and fc.fee.speciality=:sp ";
            m.put("s", currentStaff);
            m.put("sp", speciality);
        }
        m.put("ed", effectiveDate);
        List<FeeChange> changes = getFeeChangeFacade().findByJpql(sql, m, TemporalType.DATE);
        for (FeeChange fc : feeChanges) {
            if ((fc.getFee().getFee() == 0) && (fc.getFee().getFfee() == 0)) {
                continue;
            }
            fc.setValidFrom(effectiveDate);
            if (changes.size() > 0) {
                for (FeeChange c : changes) {
                    if ((fc.getFee().getFeeType() == c.getFee().getFeeType())
                            && (fc.getFee().getName().equals(c.getFee().getName()))
                            && (fc.getFee().getStaff().equals(c.getFee().getStaff()))
                            && (fc.getFee().getSpeciality().equals(c.getFee().getSpeciality()))
                            && (fc.getValidFrom().getTime() == c.getValidFrom().getTime())) {
                        JsfUtil.addErrorMessage("This Fee Already Add - " + c.getFee().getName() + " , " + c.getFee().getFeeType() + " , " + c.getValidFrom());
                    } else {
                        System.out.println("fc.getFee().getName() = " + fc.getFee().getName());
                        System.out.println("c.getFee().getName() = " + c.getFee().getName());
                        System.out.println("fc.getFee().getFeeType() = " + fc.getFee().getFeeType());
                        if ((fc.getFee().getFee() != 0 || fc.getFee().getFfee() != 0) && (fc.getFee().getFee() != c.getFee().getFee() || fc.getFee().getFfee() != fc.getFee().getFfee())) {
                            fc.setValidFrom(effectiveDate);
                            fc.setCreatedAt(new Date());
                            fc.setCreater(getSessionController().getLoggedUser());
                            getFeeFacade().create(fc.getFee());
                            getFeeChangeFacade().create(fc);
                            JsfUtil.addSuccessMessage("Fee Change Added - " + c.getFee().getName() + " , " + c.getFee().getFeeType() + " , " + c.getValidFrom());
                        } else {
                            JsfUtil.addErrorMessage("Already Added");
                        }

                    }

                }
            } else {
                if (fc.getFee().getFee() != 0 || fc.getFee().getFfee() != 0) {
                    fc.setValidFrom(effectiveDate);
                    fc.setCreatedAt(new Date());
                    fc.setCreater(getSessionController().getLoggedUser());
                    getFeeFacade().create(fc.getFee());
                    getFeeChangeFacade().create(fc);
                    JsfUtil.addSuccessMessage("Fee Change Added - " + fc.getFee().getName() + " , " + fc.getFee().getFeeType() + " , " + fc.getValidFrom());
                } else {
                    JsfUtil.addErrorMessage("Fees Zero");
                }
            }

        }
        prepareAdd();

    }

    public void createFeeChangeTable() {
        String sql;
        Map m = new HashMap();
        sql = " select fc from FeeChange fc where "
                + " fc.retired=false "
                + " and fc.validFrom>:ed ";
        m.put("ed", effectiveDate);
        feeChangesList = getFeeChangeFacade().findByJpql(sql, m, TemporalType.TIMESTAMP);
    }

    public void removeAddFee(FeeChange fc) {
        fc.setRetired(true);
        fc.setRetiredAt(new Date());
        fc.setRetirer(getSessionController().getLoggedUser());
        getFeeChangeFacade().edit(fc);
        JsfUtil.addSuccessMessage("Removed");
    }

    public void feeChangeListner() {
        if (!feeChangeStaff) {
            speciality = null;
            currentStaff = null;
        }
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {

        this.sessionController = sessionController;
    }

    public FeeFacade getFeeFacade() {
        return feeFacade;
    }

    public void setFeeFacade(FeeFacade feeFacade) {
        this.feeFacade = feeFacade;
    }

    public Staff getCurrentStaff() {
        return currentStaff;
    }

    public void setCurrentStaff(Staff currentStaff) {
        this.currentStaff = currentStaff;
    }

    public List<ServiceSession> getAllSession() {
        String sql = "Select s From ServiceSession s where s.retired=false order by s.staff.speciality.name,s.staff.person.name,s.sessionWeekday,s.startingTime ";
        List<ServiceSession> tmp = getFacade().findByJpql(sql);

        return tmp;
    }

    public List<ServiceSession> getFilteredValue() {
        return filteredValue;
    }

    public void setFilteredValue(List<ServiceSession> filteredValue) {
        this.filteredValue = filteredValue;
    }

    public Date getEffectiveDate() {
        if (effectiveDate == null) {
            effectiveDate = CommonFunctions.getEndOfDay(new Date());
        } else {
            effectiveDate = CommonFunctions.getEndOfDay(effectiveDate);
        }
        return effectiveDate;
    }

    public void setEffectiveDate(Date effectiveDate) {
        this.effectiveDate = effectiveDate;
    }

    public List<FeeChange> getFeeChanges() {
        if (feeChanges == null) {
            feeChanges = new ArrayList<>();
        }
        return feeChanges;
    }

    public void setFeeChanges(List<FeeChange> feeChanges) {
        this.feeChanges = feeChanges;
    }

    

    public FeeChangeFacade getFeeChangeFacade() {
        return feeChangeFacade;
    }

    public void setFeeChangeFacade(FeeChangeFacade feeChangeFacade) {
        this.feeChangeFacade = feeChangeFacade;
    }

    public List<FeeChange> getFeeChangesList() {
        return feeChangesList;
    }

    public void setFeeChangesList(List<FeeChange> feeChangesList) {
        this.feeChangesList = feeChangesList;
    }

    public boolean isFeeChangeStaff() {
        return feeChangeStaff;
    }

    public void setFeeChangeStaff(boolean feeChangeStaff) {
        this.feeChangeStaff = feeChangeStaff;
    }

}
