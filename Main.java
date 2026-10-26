import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.*;
import java.time.LocalDate;
import java.util.*;
import java.util.List;
import java.text.NumberFormat;

public class Main extends JFrame {
    private static final String DATA_FILE = "payroll-data.ser";
    private final List<Employee> employees = new ArrayList<>();
    private final Map<String, Attendance> attendance = new LinkedHashMap<>();
    private final NumberFormat money = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));
    private DefaultTableModel employeeModel, payrollModel, attendanceModel;
    private JTable employeeTable, payrollTable, attendanceTable;
    private JTextField employeeSearch, payrollSearch, attendanceDate;
    private JComboBox<String> attendanceEmployee, attendanceStatus;
    private JLabel employeeCount, payrollTotal, attendanceCount, leaveCount;
    private JTextArea payslipPreview;

    public Main() {
        super("Employee Payroll Management System");
        loadData();
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1120, 720);
        setMinimumSize(new Dimension(900, 600));
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        add(header(), BorderLayout.NORTH);
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Dashboard", dashboard());
        tabs.addTab("Employees", employeesPanel());
        tabs.addTab("Payroll & Payslips", payrollPanel());
        tabs.addTab("Attendance", attendancePanel());
        add(tabs, BorderLayout.CENTER);
        refreshAll();
    }

    private JPanel header() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(new Color(31, 57, 95));
        p.setBorder(BorderFactory.createEmptyBorder(15, 22, 15, 22));
        JLabel title = new JLabel("Employee Payroll Management System");
        title.setForeground(Color.WHITE); title.setFont(new Font("SansSerif", Font.BOLD, 23));
        JLabel sub = new JLabel("Employee records  •  Salary calculations  •  Payslips  •  Attendance");
        sub.setForeground(new Color(220, 230, 245));
        JPanel text = new JPanel(new GridLayout(2, 1, 0, 5)); text.setOpaque(false);
        text.add(title); text.add(sub); p.add(text, BorderLayout.WEST);
        return p;
    }

    private JPanel dashboard() {
        JPanel p = new JPanel(new BorderLayout(14, 14));
        p.setBorder(BorderFactory.createEmptyBorder(22, 22, 22, 22));
        p.setBackground(new Color(245, 247, 251));
        JPanel cards = new JPanel(new GridLayout(1, 4, 12, 12)); cards.setOpaque(false);
        employeeCount = new JLabel("0"); payrollTotal = new JLabel(money.format(0));
        attendanceCount = new JLabel("0"); leaveCount = new JLabel("0");
        cards.add(card("Employees", employeeCount, new Color(43, 103, 185)));
        cards.add(card("Estimated Monthly Payroll", payrollTotal, new Color(28, 135, 100)));
        cards.add(card("Attendance Records", attendanceCount, new Color(190, 119, 34)));
        cards.add(card("Leave Records", leaveCount, new Color(125, 78, 160)));
        JTextArea help = new JTextArea(
            "WELCOME TO YOUR PAYROLL APP\n\n" +
            "EMPLOYEES\nAdd, edit, delete, and search employee records. Each employee has salary components.\n\n" +
            "PAYROLL & PAYSLIPS\nReview salary calculations, generate a payslip preview, and export the payroll table to CSV.\n\n" +
            "ATTENDANCE\nRecord attendance for a date as Present, Absent, or Leave. One record is stored per employee per date.\n\n" +
            "SALARY FORMULAS\nGross = Basic + HRA + Other Allowance\nTotal Deductions = PF + Tax + Other Deductions\nNet Salary = Gross - Total Deductions\n\n" +
            "Data is saved locally in payroll-data.ser in the folder where you run the app.");
        help.setEditable(false); help.setFont(new Font("SansSerif", Font.PLAIN, 14));
        help.setBackground(Color.WHITE); help.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        p.add(cards, BorderLayout.NORTH); p.add(new JScrollPane(help), BorderLayout.CENTER);
        return p;
    }

    private JPanel card(String title, JLabel value, Color accent) {
        JPanel p = new JPanel(new BorderLayout(6, 12)); p.setBackground(Color.WHITE);
        p.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0,0,4,0,accent),
                BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(225,230,238)),
                        BorderFactory.createEmptyBorder(16,14,16,14))));
        JLabel t = new JLabel(title); t.setForeground(new Color(85,95,110)); t.setFont(new Font("SansSerif",Font.BOLD,12));
        value.setForeground(accent); value.setFont(new Font("SansSerif",Font.BOLD,21));
        p.add(t, BorderLayout.NORTH); p.add(value, BorderLayout.CENTER); return p;
    }

    private JPanel employeesPanel() {
        JPanel p = new JPanel(new BorderLayout(8, 10)); p.setBorder(BorderFactory.createEmptyBorder(14,14,14,14));
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton add = btn("Add Employee"), edit = btn("Edit Selected"), del = btn("Delete Selected");
        JButton search = btn("Search"), clear = btn("Clear");
        employeeSearch = new JTextField(16);
        bar.add(add); bar.add(edit); bar.add(del); bar.add(new JLabel("Search ID/name:"));
        bar.add(employeeSearch); bar.add(search); bar.add(clear);
        employeeModel = model("ID","Name","Department","Designation","Phone","Net Salary");
        employeeTable = table(employeeModel);
        p.add(bar, BorderLayout.NORTH); p.add(new JScrollPane(employeeTable), BorderLayout.CENTER);
        add.addActionListener(e -> employeeDialog(null));
        edit.addActionListener(e -> { Employee x = selected(employeeTable, employeeModel); if(x==null) msg("Select an employee."); else employeeDialog(x); });
        del.addActionListener(e -> { Employee x=selected(employeeTable,employeeModel); if(x==null){msg("Select an employee.");return;}
            if(JOptionPane.showConfirmDialog(this,"Delete "+x.name+"?","Confirm",JOptionPane.YES_NO_OPTION)==JOptionPane.YES_OPTION){
                employees.remove(x); attendance.entrySet().removeIf(a -> a.getValue().id.equals(x.id)); saveData(); refreshAll(); }});
        search.addActionListener(e -> refreshEmployees(employeeSearch.getText()));
        employeeSearch.addActionListener(e -> refreshEmployees(employeeSearch.getText()));
        clear.addActionListener(e -> {employeeSearch.setText("");refreshEmployees("");});
        return p;
    }

    private JPanel payrollPanel() {
        JPanel p = new JPanel(new BorderLayout(8,10)); p.setBorder(BorderFactory.createEmptyBorder(14,14,14,14));
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        payrollSearch = new JTextField(18);
        JButton search=btn("Search"), clear=btn("Clear"), slip=btn("Generate Payslip"), export=btn("Export CSV");
        bar.add(new JLabel("Search ID/name:")); bar.add(payrollSearch); bar.add(search); bar.add(clear); bar.add(slip); bar.add(export);
        payrollModel=model("ID","Employee","Basic","HRA","Allowance","Gross","PF","Tax","Other Ded.","Net Salary");
        payrollTable=table(payrollModel);
        payslipPreview=new JTextArea(); payslipPreview.setEditable(false); payslipPreview.setFont(new Font(Font.MONOSPACED,Font.PLAIN,13));
        payslipPreview.setBorder(BorderFactory.createEmptyBorder(8,8,8,8));
        JScrollPane preview=new JScrollPane(payslipPreview); preview.setBorder(BorderFactory.createTitledBorder("Payslip Preview"));
        JSplitPane split=new JSplitPane(JSplitPane.VERTICAL_SPLIT,new JScrollPane(payrollTable),preview); split.setResizeWeight(.68);
        p.add(bar,BorderLayout.NORTH); p.add(split,BorderLayout.CENTER);
        search.addActionListener(e -> refreshPayroll(payrollSearch.getText()));
        payrollSearch.addActionListener(e -> refreshPayroll(payrollSearch.getText()));
        clear.addActionListener(e -> {payrollSearch.setText("");refreshPayroll("");});
        slip.addActionListener(e -> {Employee x=selected(payrollTable,payrollModel);if(x==null)msg("Select an employee in the payroll table.");else payslipPreview.setText(payslip(x));});
        export.addActionListener(e -> exportCsv());
        return p;
    }

    private JPanel attendancePanel() {
        JPanel p = new JPanel(new BorderLayout(8,10)); p.setBorder(BorderFactory.createEmptyBorder(14,14,14,14));
        JPanel bar=new JPanel(new FlowLayout(FlowLayout.LEFT));
        attendanceEmployee=new JComboBox<>(); attendanceDate=new JTextField(LocalDate.now().toString(),10);
        attendanceStatus=new JComboBox<>(new String[]{"Present","Absent","Leave"});
        JButton save=btn("Save Attendance"), del=btn("Delete Selected");
        bar.add(new JLabel("Employee:"));bar.add(attendanceEmployee);bar.add(new JLabel("Date YYYY-MM-DD:"));
        bar.add(attendanceDate);bar.add(new JLabel("Status:"));bar.add(attendanceStatus);bar.add(save);bar.add(del);
        attendanceModel=model("Date","Employee ID","Employee Name","Status");attendanceTable=table(attendanceModel);
        p.add(bar,BorderLayout.NORTH);p.add(new JScrollPane(attendanceTable),BorderLayout.CENTER);
        save.addActionListener(e -> saveAttendance());
        del.addActionListener(e -> {int r=attendanceTable.getSelectedRow();if(r<0){msg("Select an attendance record.");return;}
            attendance.remove(attendanceModel.getValueAt(r,0)+"|"+attendanceModel.getValueAt(r,1));saveData();refreshAll();});
        return p;
    }

    private void employeeDialog(Employee old) {
        JTextField[] f=new JTextField[11];
        String[] labels={"Employee ID *","Full name *","Department","Designation","Phone","Basic salary","HRA","Other allowance","PF deduction","Tax deduction","Other deduction"};
        String[] values=old==null?new String[]{"","","","","","25000","5000","2000","2000","0","500"}:
            new String[]{old.id,old.name,old.department,old.designation,old.phone,""+old.basic,""+old.hra,""+old.allowance,""+old.pf,""+old.tax,""+old.other};
        JPanel form=new JPanel(new GridLayout(0,2,8,7));
        for(int i=0;i<f.length;i++){f[i]=new JTextField(values[i]);form.add(new JLabel(labels[i]));form.add(f[i]);}
        int result=JOptionPane.showConfirmDialog(this,form,old==null?"Add Employee":"Edit Employee",JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE);
        if(result!=JOptionPane.OK_OPTION)return;
        try {
            String id=f[0].getText().trim(), name=f[1].getText().trim();
            if(id.isEmpty()||name.isEmpty())throw new IllegalArgumentException("Employee ID and name are required.");
            for(Employee e:employees)if(e.id.equalsIgnoreCase(id)&&e!=old)throw new IllegalArgumentException("Employee ID already exists.");
            double[] n=new double[6];for(int i=0;i<6;i++)n[i]=amount(f[i+5].getText(),labels[i+5]);
            if(old==null)employees.add(new Employee(id,name,f[2].getText().trim(),f[3].getText().trim(),f[4].getText().trim(),n));
            else {old.name=name;old.department=f[2].getText().trim();old.designation=f[3].getText().trim();old.phone=f[4].getText().trim();
                old.basic=n[0];old.hra=n[1];old.allowance=n[2];old.pf=n[3];old.tax=n[4];old.other=n[5];}
            saveData();refreshAll();
        } catch(IllegalArgumentException ex){msg(ex.getMessage());}
    }

    private double amount(String s,String label) {
        try {double d=Double.parseDouble(s.trim());if(!Double.isFinite(d)||d<0)throw new NumberFormatException();return d;}
        catch(Exception ex){throw new IllegalArgumentException(label+" must be a valid non-negative number.");}
    }

    private void saveAttendance() {
        if(employees.isEmpty()){msg("Add an employee first.");return;}
        Object choice=attendanceEmployee.getSelectedItem();if(choice==null){msg("Select an employee.");return;}
        try {
            LocalDate date=LocalDate.parse(attendanceDate.getText().trim());
            String id=choice.toString().split(" - ",2)[0];Employee e=find(id);if(e==null)return;
            attendance.put(date+"|"+id,new Attendance(date.toString(),id,e.name,(String)attendanceStatus.getSelectedItem()));
            saveData();refreshAll();msg("Attendance saved.");
        } catch(Exception ex){msg("Enter date as YYYY-MM-DD, for example 2026-10-26.");}
    }

    private void refreshAll(){refreshEmployees("");refreshPayroll("");refreshAttendance();refreshCombo();
        if(employeeCount!=null)employeeCount.setText(""+employees.size());
        if(payrollTotal!=null)payrollTotal.setText(money.format(employees.stream().mapToDouble(Employee::net).sum()));
        if(attendanceCount!=null)attendanceCount.setText(""+attendance.size());
        if(leaveCount!=null)leaveCount.setText(""+attendance.values().stream().filter(a->a.status.equals("Leave")).count());}
    private void refreshEmployees(String q){if(employeeModel==null)return;employeeModel.setRowCount(0);for(Employee e:employees)if(matches(e,q))
        employeeModel.addRow(new Object[]{e.id,e.name,e.department,e.designation,e.phone,money.format(e.net())});}
    private void refreshPayroll(String q){if(payrollModel==null)return;payrollModel.setRowCount(0);for(Employee e:employees)if(matches(e,q))
        payrollModel.addRow(new Object[]{e.id,e.name,money.format(e.basic),money.format(e.hra),money.format(e.allowance),money.format(e.gross()),money.format(e.pf),money.format(e.tax),money.format(e.other),money.format(e.net())});}
    private void refreshAttendance(){if(attendanceModel==null)return;attendanceModel.setRowCount(0);
        List<Attendance> list=new ArrayList<>(attendance.values());list.sort(Comparator.comparing((Attendance a)->a.date).reversed());
        for(Attendance a:list)attendanceModel.addRow(new Object[]{a.date,a.id,a.name,a.status});}
    private void refreshCombo(){if(attendanceEmployee==null)return;Object prev=attendanceEmployee.getSelectedItem();attendanceEmployee.removeAllItems();
        for(Employee e:employees)attendanceEmployee.addItem(e.id+" - "+e.name);if(prev!=null)attendanceEmployee.setSelectedItem(prev);}
    private boolean matches(Employee e,String q){if(q==null||q.trim().isEmpty())return true;q=q.toLowerCase(Locale.ROOT);
        return e.id.toLowerCase(Locale.ROOT).contains(q)||e.name.toLowerCase(Locale.ROOT).contains(q)||e.department.toLowerCase(Locale.ROOT).contains(q);}
    private Employee find(String id){for(Employee e:employees)if(e.id.equalsIgnoreCase(id))return e;return null;}
    private Employee selected(JTable t,DefaultTableModel m){int r=t.getSelectedRow();if(r<0)return null;return find(m.getValueAt(r,0).toString());}
    private String payslip(Employee e){String line="--------------------------------------------------\n";
        return "             EMPLOYEE SALARY SLIP\n"+line+"Date          : "+LocalDate.now()+"\nEmployee ID   : "+e.id+"\nEmployee Name : "+e.name+
        "\nDepartment    : "+e.department+"\nDesignation   : "+e.designation+"\n"+line+
        String.format("%-28s %18s%n","Basic Salary",money.format(e.basic))+
        String.format("%-28s %18s%n","HRA",money.format(e.hra))+
        String.format("%-28s %18s%n","Other Allowance",money.format(e.allowance))+
        String.format("%-28s %18s%n","Gross Salary",money.format(e.gross()))+line+
        String.format("%-28s %18s%n","PF Deduction",money.format(e.pf))+
        String.format("%-28s %18s%n","Tax Deduction",money.format(e.tax))+
        String.format("%-28s %18s%n","Other Deduction",money.format(e.other))+
        String.format("%-28s %18s%n","Total Deductions",money.format(e.deductions()))+line+
        String.format("%-28s %18s%n","NET SALARY",money.format(e.net()))+line+"Sample payslip for educational use.";}
    private void exportCsv(){JFileChooser c=new JFileChooser();c.setSelectedFile(new File("payroll-report.csv"));
        if(c.showSaveDialog(this)!=JFileChooser.APPROVE_OPTION)return;
        try(PrintWriter w=new PrintWriter(new OutputStreamWriter(new FileOutputStream(c.getSelectedFile()),"UTF-8"))){
            w.println("ID,Name,Department,Designation,Basic,HRA,Allowance,Gross,PF,Tax,Other Deduction,Net");
            for(Employee e:employees)w.printf(Locale.US,"%s,%s,%s,%s,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f%n",
                csv(e.id),csv(e.name),csv(e.department),csv(e.designation),e.basic,e.hra,e.allowance,e.gross(),e.pf,e.tax,e.other,e.net());
            msg("CSV exported successfully.");}catch(Exception ex){msg("Export failed: "+ex.getMessage());}}
    private String csv(String s){return "\""+s.replace("\"","\"\"")+"\"";}
    private DefaultTableModel model(String... cols){return new DefaultTableModel(cols,0){public boolean isCellEditable(int r,int c){return false;}};}
    private JTable table(DefaultTableModel m){JTable t=new JTable(m);t.setRowHeight(26);t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);t.setAutoCreateRowSorter(true);return t;}
    private JButton btn(String s){JButton b=new JButton(s);b.setFocusPainted(false);return b;}
    private void msg(String s){JOptionPane.showMessageDialog(this,s);}
    @SuppressWarnings("unchecked") private void loadData(){File f=new File(DATA_FILE);if(!f.exists())return;
        try(ObjectInputStream in=new ObjectInputStream(new FileInputStream(f))){employees.addAll((List<Employee>)in.readObject());attendance.putAll((Map<String,Attendance>)in.readObject());}
        catch(Exception ex){JOptionPane.showMessageDialog(null,"Saved data could not be loaded. Starting with empty records.\n"+ex.getMessage());}}
    private void saveData(){try(ObjectOutputStream out=new ObjectOutputStream(new FileOutputStream(DATA_FILE))){out.writeObject(new ArrayList<>(employees));out.writeObject(new LinkedHashMap<>(attendance));}
        catch(IOException ex){msg("Could not save data: "+ex.getMessage());}}

    private static class Employee implements Serializable {
        private static final long serialVersionUID=1L;
        String id,name,department,designation,phone;double basic,hra,allowance,pf,tax,other;
        Employee(String id,String name,String department,String designation,String phone,double[] n){
            this.id=id;this.name=name;this.department=department;this.designation=designation;this.phone=phone;
            basic=n[0];hra=n[1];allowance=n[2];pf=n[3];tax=n[4];other=n[5];}
        double gross(){return basic+hra+allowance;}double deductions(){return pf+tax+other;}double net(){return gross()-deductions();}
    }
    private static class Attendance implements Serializable {
        private static final long serialVersionUID=1L;String date,id,name,status;
        Attendance(String date,String id,String name,String status){this.date=date;this.id=id;this.name=name;this.status=status;}
    }
    public static void main(String[] args){SwingUtilities.invokeLater(()->{try{UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());}catch(Exception ignored){}new Main().setVisible(true);});}
}
