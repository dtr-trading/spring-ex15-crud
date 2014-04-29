package com.dtr.oas.dao;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.dtr.oas.exception.DuplicateStrategyException;
import com.dtr.oas.exception.StrategyNotFoundException;
import com.dtr.oas.model.Strategy;

@Repository
public class StrategyDAOImpl implements StrategyDAO {

    @Autowired
    private SessionFactory sessionFactory;

    private Session getCurrentSession() {
        return sessionFactory.getCurrentSession();
    }

    public void addStrategy(Strategy strategy) throws DuplicateStrategyException {
        List<Strategy> strategies = getStrategies();
        for (Strategy singleStrategy : strategies) {
            if (singleStrategy.getType().equals(strategy.getType()) && singleStrategy.getName().equals(strategy.getName())) {
                String message = "The Strategy [" + singleStrategy.getName() + "] already exists in the system.";
                throw new DuplicateStrategyException(message);
            }
        }
        getCurrentSession().save(strategy);
    }

    public void updateStrategy(Strategy strategy) throws StrategyNotFoundException, DuplicateStrategyException {
        Strategy strategyToUpdate = getStrategy(strategy.getId());
        
        if (strategyToUpdate.getType().equals(strategy.getType()) && strategyToUpdate.getName().equals(strategy.getName())) {
            String message = "The Strategy [" + strategyToUpdate.getName() + "] already exists in the system.";
            throw new DuplicateStrategyException(message);
        } else {
            strategyToUpdate.setName(strategy.getName());
            strategyToUpdate.setType(strategy.getType());
            getCurrentSession().update(strategyToUpdate);
        }

    }

    public Strategy getStrategy(int id) throws StrategyNotFoundException {
        Strategy strategy = (Strategy) getCurrentSession().get(Strategy.class, id);
        
        if (strategy == null) {
            throw new StrategyNotFoundException("Strategy id [" + id + "] not found in the system.");
        } else {
            return strategy;
        }
    }

    public void deleteStrategy(int id) throws StrategyNotFoundException {
        Strategy strategy = getStrategy(id);
        if (strategy != null)
            getCurrentSession().delete(strategy);
    }

    @SuppressWarnings("unchecked")
    public List<Strategy> getStrategies() {
        return getCurrentSession().createQuery("from Strategy").list();
    }

}
