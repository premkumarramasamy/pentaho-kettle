package org.pentaho.di.engine.kettlenative.impl;

import org.junit.Before;
import org.junit.Test;
import org.pentaho.di.core.KettleEnvironment;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.exception.KettleMissingPluginsException;
import org.pentaho.di.core.exception.KettleXMLException;
import org.pentaho.di.engine.api.ExecutionContext;
import org.pentaho.di.engine.api.ExecutionResult;
import org.pentaho.di.engine.api.model.Operation;
import org.pentaho.di.engine.api.model.Transformation;
import org.pentaho.di.engine.api.reporting.Metrics;
import org.pentaho.di.trans.TransMeta;

import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class EngineITest {

  TransMeta testMeta;
  static Engine engine = new Engine();

  @Before
  public void before() throws KettleException {
    KettleEnvironment.init();
    testMeta = new TransMeta( getClass().getClassLoader().getResource( "test2.ktr" ).getFile() );
  }

  @Test
  public void testExec() throws KettleXMLException, KettleMissingPluginsException, InterruptedException {
    TransMeta meta = new TransMeta( getClass().getClassLoader().getResource( "lorem.ktr" ).getFile() );
    Transformation trans = org.pentaho.di.engine.kettlenative.impl.Transformation.convert( meta );
    ExecutionContext executionContext = engine.prepare( trans );
    engine.execute( executionContext );
  }

  @Test
  public void test2Sources1Sink()
    throws KettleXMLException, KettleMissingPluginsException, InterruptedException, ExecutionException {
    ExecutionResult result = getTestExecutionResult( "2InputsWithConsistentColumns.ktr" );
    Map<Operation, Metrics> reports = result.getDataEventReport();
    assertThat( reports.size(), is( 3 ) );
    Metrics dataGrid1 = getByName( "Data Grid", reports );
    Metrics dataGrid2 = getByName( "Data Grid 2", reports );
    Metrics dummy = getByName( "Dummy (do nothing)", reports );
    System.out.println( reports );
    assertThat( dataGrid1.getOut(), is( 1l ) );
    assertThat( dataGrid2.getOut(), is( 1l ) );
    assertThat( "dummy should get rows fromm both data grids", dummy.getIn(), is( 2l ) );
    System.out.println( reports );
  }

  @Test
  public void test1source2trans1sink()
    throws KettleXMLException, KettleMissingPluginsException, InterruptedException, ExecutionException {
    ExecutionResult result = getTestExecutionResult( "1source.2Trans.1sink.ktr" );
    Map<Operation, Metrics> reports = result.getDataEventReport();
    assertThat( reports.size(), is( 5 ) );
    System.out.println( reports );

  }

  @Test
  public void simpleFilter()
    throws KettleXMLException, KettleMissingPluginsException, InterruptedException, ExecutionException {
    ExecutionResult result = getTestExecutionResult( "simpleFilter.ktr" );
    Map<Operation, Metrics> reports = result.getDataEventReport();
    System.out.println( reports );

  }

  @Test
  public void testLookup()
    throws KettleXMLException, KettleMissingPluginsException, InterruptedException, ExecutionException {
    ExecutionResult result = getTestExecutionResult( "SparkSample.ktr" );
    Map<Operation, Metrics> reports = result.getDataEventReport();
    Thread.sleep( 100 );  // Don't check before file is done being written
    assertThat( getByName( "Merged Output", reports ).getOut(), is( 2001l ) );  // hmm, out + written
    System.out.println( reports );
  }


  @Test
  public void testChainedCalc()
    throws KettleXMLException, KettleMissingPluginsException, InterruptedException, ExecutionException {
    // executes a series of Calculation steps as a chain of Spark FlatMapFunctions.
    ExecutionResult result = getTestExecutionResult( "StringCalc.ktr" );
    Map<Operation, Metrics> reports = result.getDataEventReport();
    System.out.println( reports );

  }

  private ExecutionResult getTestExecutionResult( String transName )
    throws KettleXMLException, KettleMissingPluginsException, InterruptedException,
    ExecutionException {
    TransMeta meta = new TransMeta( getClass().getClassLoader().getResource( transName ).getFile() );
    Transformation trans = org.pentaho.di.engine.kettlenative.impl.Transformation.convert( meta );
    ExecutionContext executionContext = engine.prepare( trans );
    Future<ExecutionResult> resultFuture = engine.execute( executionContext );
    return resultFuture.get();
  }

  private Metrics getByName( String name, Map<Operation, Metrics> reports ) {
    return reports.keySet().stream()
      .filter( report -> report.getId().equals( name ) )
      .findFirst()
      .map( reports::get )
      .orElseThrow( () -> new AssertionError( name + " not found in " + reports ) );
  }


}