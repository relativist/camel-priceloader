package sample.camel.internalBeans;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ValidatorBeanExpression {
    private ExcelConverterBean excelConverterBean;
    private JdbcStore store;

    public ValidatorBeanExpression(ExcelConverterBean excelConverterBean, JdbcStore store) {
        this.excelConverterBean = excelConverterBean;
        this.store = store;
    }

//    public boolean isCompletedIterator(){
//        return excelConverterBean.isCompletedIterator();
//    }


//    public boolean isCompletedStore(){
//        final boolean allStored = store.getTotalStored() >= excelConverterBean.getTotalRead();
//        if (excelConverterBean.isCompletedIterator()) {
//            log.info("isCompletedStore (iterator:{}/read:{}/stored:{}/result:{})",
//                    excelConverterBean.isCompletedIterator(),
//                    excelConverterBean.getTotalRead(),
//                    store.getTotalStored(),
//                    allStored);
//        }
//        return excelConverterBean.isCompletedIterator() && allStored;
//    }
}
