
package org.drip.execution.optimum;

/*
 * -*- mode: java; tab-width: 4; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 */

/*!
 * Copyright (C) 2017 Lakshmi Krishnamurthy
 * Copyright (C) 2016 Lakshmi Krishnamurthy
 * 
 *  This file is part of DRIP, a free-software/open-source library for buy/side financial/trading model
 *  	libraries targeting analysts and developers
 *  	https://lakshmidrip.github.io/DRIP/
 *  
 *  DRIP is composed of four main libraries:
 *  
 *  - DRIP Fixed Income - https://lakshmidrip.github.io/DRIP-Fixed-Income/
 *  - DRIP Asset Allocation - https://lakshmidrip.github.io/DRIP-Asset-Allocation/
 *  - DRIP Numerical Optimizer - https://lakshmidrip.github.io/DRIP-Numerical-Optimizer/
 *  - DRIP Statistical Learning - https://lakshmidrip.github.io/DRIP-Statistical-Learning/
 * 
 *  - DRIP Fixed Income: Library for Instrument/Trading Conventions, Treasury Futures/Options,
 *  	Funding/Forward/Overnight Curves, Multi-Curve Construction/Valuation, Collateral Valuation and XVA
 *  	Metric Generation, Calibration and Hedge Attributions, Statistical Curve Construction, Bond RV
 *  	Metrics, Stochastic Evolution and Option Pricing, Interest Rate Dynamics and Option Pricing, LMM
 *  	Extensions/Calibrations/Greeks, Algorithmic Differentiation, and Asset Backed Models and Analytics.
 * 
 *  - DRIP Asset Allocation: Library for model libraries for MPT framework, Black Litterman Strategy
 *  	Incorporator, Holdings Constraint, and Transaction Costs.
 * 
 *  - DRIP Numerical Optimizer: Library for Numerical Optimization and Spline Functionality.
 * 
 *  - DRIP Statistical Learning: Library for Statistical Evaluation and Machine Learning.
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *   	you may not use this file except in compliance with the License.
 *   
 *  You may obtain a copy of the License at
 *  	http://www.apache.org/licenses/LICENSE-2.0
 *  
 *  Unless required by applicable law or agreed to in writing, software
 *  	distributed under the License is distributed on an "AS IS" BASIS,
 *  	WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  
 *  See the License for the specific language governing permissions and
 *  	limitations under the License.
 */

/**
 * TradingEnhancedDiscrete contains the Trading Trajectory generated by one of the Methods outlined in the
 *  Almgren (2003) Scheme for Continuous Trading Approximation for Linear Trading Enhanced Temporary Impact
 *  Volatility. The References are:
 * 
 * 	- Almgren, R., and N. Chriss (1999): Value under Liquidation, Risk 12 (12).
 * 
 * 	- Almgren, R., and N. Chriss (2000): Optimal Execution of Portfolio Transactions, Journal of Risk 3 (2)
 * 		5-39.
 * 
 * 	- Almgren, R. (2003): Optimal Execution with Nonlinear Impact Functions and Trading-Enhanced Risk,
 * 		Applied Mathematical Finance 10 (1) 1-18.
 * 
 * 	- Almgren, R., and N. Chriss (2003): Bidding Principles, Risk 97-102.
 * 
 * 	- Bertsimas, D., and A. W. Lo (1998): Optimal Control of Execution Costs, Journal of Financial Markets,
 * 		1, 1-50.
 * 
 * @author Lakshmi Krishnamurthy
 */

public class TradingEnhancedDiscrete extends org.drip.execution.optimum.EfficientTradingTrajectoryDiscrete {
	private double _dblCharacteristicSize = java.lang.Double.NaN;
	private double _dblCharacteristicTime = java.lang.Double.NaN;

	/**
	 * Construct a Standard TradingEnhancedDiscrete Instance
	 * 
	 * @param dtt The Trading Trajectory
	 * @param apep The Arithmetic Price Walk Evolution Parameters
	 * @param dblCharacteristicTime The Optimal Trajectory's "Characteristic" Time
	 * @param dblCharacteristicSize The Optimal Trajectory's "Characteristic" Size
	 * 
	 * @return The TradingEnhancedDiscrete Instance
	 */

	public static TradingEnhancedDiscrete Standard (
		final org.drip.execution.strategy.DiscreteTradingTrajectory dtt,
		final org.drip.execution.dynamics.ArithmeticPriceEvolutionParameters apep,
		final double dblCharacteristicTime,
		final double dblCharacteristicSize)
	{
		if (null == dtt || null == apep) return null;

		try {
			org.drip.measure.gaussian.R1UnivariateNormal r1un = (new
				org.drip.execution.capture.TrajectoryShortfallEstimator (dtt)).totalCostDistributionSynopsis
					(apep);

			return null == r1un ? null : new TradingEnhancedDiscrete (dtt.executionTimeNode(),
				dtt.holdings(), dtt.tradeList(), r1un.mean(), r1un.variance(), dblCharacteristicTime,
					dblCharacteristicSize, apep.temporaryExpectation().epochImpactFunction().evaluate
						(dtt.instantTradeRate()) / (apep.arithmeticPriceDynamicsSettings().epochVolatility()
							* java.lang.Math.sqrt (dtt.executionTime())));
		} catch (java.lang.Exception e) {
			e.printStackTrace();
		}

		return null;
	}

	/**
	 * TradingEnhancedDiscrete Constructor
	 * 
	 * @param adblExecutionTimeNode Array containing the Trajectory Time Nodes
	 * @param adblHoldings Array containing the Holdings
	 * @param adblTradeList Array containing the Trade List
	 * @param dblTransactionCostExpectation The Expected Transaction Cost
	 * @param dblTransactionCostVariance The Variance of the Transaction Cost
	 * @param dblCharacteristicTime The Optimal Trajectory's "Characteristic" Time
	 * @param dblCharacteristicSize The Optimal Trajectory's "Characteristic" Size
	 * @param dblMarketPower Estimate of the Relative Market Impact Power
	 * 
	 * @throws java.lang.Exception Thrown if the Inputs are Invalid
	 */

	public TradingEnhancedDiscrete (
		final double[] adblExecutionTimeNode,
		final double[] adblHoldings,
		final double[] adblTradeList,
		final double dblTransactionCostExpectation,
		final double dblTransactionCostVariance,
		final double dblCharacteristicTime,
		final double dblCharacteristicSize,
		final double dblMarketPower)
		throws java.lang.Exception
	{
		super (adblExecutionTimeNode, adblHoldings, adblTradeList, dblTransactionCostExpectation,
			dblTransactionCostVariance, dblMarketPower);

		if (!org.drip.quant.common.NumberUtil.IsValid (_dblCharacteristicTime = dblCharacteristicTime) ||
			!org.drip.quant.common.NumberUtil.IsValid (_dblCharacteristicSize = dblCharacteristicSize))
			throw new java.lang.Exception ("TradingEnhancedDiscrete Constructor => Invalid Inputs");
	}

	/**
	 * Retrieve the Optimal Trajectory Characteristic Size
	 * 
	 * @return The Optimal Trajectory Characteristic Size
	 */

	public double characteristicSize()
	{
		return _dblCharacteristicSize;
	}

	/**
	 * Retrieve the Optimal Trajectory Characteristic Time
	 * 
	 * @return The Optimal Trajectory Characteristic Time
	 */

	public double characteristicTime()
	{
		return _dblCharacteristicTime;
	}
}
